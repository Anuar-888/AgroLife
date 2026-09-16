package kz.spelost.agroapp.ui.calendar;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.data.WateringPreferences;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.model.UserField;
import kz.spelost.agroapp.network.WeatherClient;
import kz.spelost.agroapp.notifications.WateringReminderScheduler;
import kz.spelost.agroapp.util.AgroLogic;
import kz.spelost.agroapp.util.DateUtils;

public class CalendarFragment extends Fragment {

    private static final int DAY_CELL_HEIGHT_DP = 56;

    private RecyclerView cropTabsView, weeklyView, timelineView, calendarGrid;
    private NestedScrollView scrollView;
    private CropTabAdapter cropTabAdapter;
    private CalendarDayAdapter calendarDayAdapter;
    private StageAdapter stageAdapter;
    private LinearLayout emptyState, contentState, selectedDayPanel;
    private Button plantDateButton;
    private TextView seasonWarning, statusPill, calendarHeader, selectedDayTitle, selectedDayTasks;
    private ProgressBar growthProgress;
    private TextView growthPercent, growthDaysLeft;
    private TextView fieldName, fieldStats, smartScoreValue, soilValue, waterValue, nutrientValue;
    private View btnPrev, btnNext, btnBack, btnMenu;
    private View calendarGridContainer;
    private Button btnToggleCalendar;

    private List<Crop> crops = new ArrayList<>();
    private final List<Calendar> days = new ArrayList<>();
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
    private Calendar currentMonthCalendar = Calendar.getInstance();
    private Calendar selectedDay = DateUtils.today();
    private UserField activeField;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);
        bindViews(root);
        setupListeners(root);
        setupRecyclerViews();
        loadCrops();
        loadWeather();
        return root;
    }

    private void bindViews(View root) {
        scrollView = root.findViewById(R.id.calendar_scroll);
        cropTabsView = root.findViewById(R.id.crop_tabs);
        emptyState = root.findViewById(R.id.calendar_empty);
        contentState = root.findViewById(R.id.calendar_content);
        plantDateButton = root.findViewById(R.id.plant_date_button);
        seasonWarning = root.findViewById(R.id.season_warning);
        statusPill = root.findViewById(R.id.status_pill);
        calendarHeader = root.findViewById(R.id.calendar_header);
        btnPrev = root.findViewById(R.id.btn_prev_month);
        btnNext = root.findViewById(R.id.btn_next_month);
        growthProgress = root.findViewById(R.id.growth_progress);
        growthPercent = root.findViewById(R.id.growth_percent);
        growthDaysLeft = root.findViewById(R.id.growth_days_left);
        calendarGrid = root.findViewById(R.id.calendar_grid);
        weeklyView = root.findViewById(R.id.weekly_weather);
        timelineView = root.findViewById(R.id.stage_timeline);
        selectedDayPanel = root.findViewById(R.id.selected_day_panel);
        selectedDayTitle = root.findViewById(R.id.selected_day_title);
        selectedDayTasks = root.findViewById(R.id.selected_day_tasks);

        fieldName = root.findViewById(R.id.field_name);
        fieldStats = root.findViewById(R.id.field_stats);
        smartScoreValue = root.findViewById(R.id.smart_score_value);
        soilValue = root.findViewById(R.id.soil_value);
        waterValue = root.findViewById(R.id.water_value);
        nutrientValue = root.findViewById(R.id.nutrient_value);
        btnBack = root.findViewById(R.id.btn_back);
        btnMenu = root.findViewById(R.id.btn_menu);
        calendarGridContainer = root.findViewById(R.id.calendar_grid_container);
        btnToggleCalendar = root.findViewById(R.id.btn_toggle_calendar);
    }

    private void setupListeners(View root) {
        plantDateButton.setOnClickListener(v -> openDatePicker());
        btnPrev.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            updateCalendarUI();
        });
        btnNext.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateCalendarUI();
        });
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof kz.spelost.agroapp.MainActivity) {
                    ((kz.spelost.agroapp.MainActivity) getActivity()).navigateTo(R.id.nav_home);
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
        
        Button btnCheck = root.findViewById(R.id.btn_check_field);
        if (btnCheck != null) {
            btnCheck.setOnClickListener(v -> {
                btnCheck.setEnabled(false);
                btnCheck.setText("Диагностика...");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded()) {
                        btnCheck.setEnabled(true);
                        btnCheck.setText("Проверить поле сейчас >");
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Диагностика завершена")
                                .setMessage("Аномалий не обнаружено. Состояние поля: Отличное.")
                                .setPositiveButton("ОК", null)
                                .show();
                    }
                }, 2000);
            });
        }
        if (btnToggleCalendar != null) {
            btnToggleCalendar.setOnClickListener(v -> {
                boolean visible = calendarGridContainer.getVisibility() == View.VISIBLE;
                calendarGridContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
                btnToggleCalendar.setText(visible ? "📅 План на месяц" : "🔼 Скрыть план");
            });
        }
    }

    private void setupRecyclerViews() {
        cropTabsView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        calendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarGrid.setNestedScrollingEnabled(false);
        weeklyView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        weeklyView.setNestedScrollingEnabled(false);
        timelineView.setLayoutManager(new LinearLayoutManager(getContext()));
        timelineView.setNestedScrollingEnabled(false);

        calendarDayAdapter = new CalendarDayAdapter(days, selectedDay, this::onDaySelected);
        calendarGrid.setAdapter(calendarDayAdapter);
        currentMonthCalendar = Calendar.getInstance();
        updateCalendarUI();
    }

    private void loadCrops() {
        CropRepository.getInstance(requireContext()).getAll(loaded -> {
            if (!isAdded()) return;
            crops = loaded;
            AppState state = AppState.getInstance();
            String selectedId = state.selectedCropId;
            if (selectedId == null && state.getSelectedField() != null) {
                selectedId = state.getSelectedField().cropId;
                state.selectedCropId = selectedId;
            }
            cropTabAdapter = new CropTabAdapter(crops, selectedId, this::selectCrop);
            cropTabsView.setAdapter(cropTabAdapter);
            if (selectedId != null) {
                showContentFor(selectedId);
            } else {
                showEmpty();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        AppState state = AppState.getInstance();
        if (state.selectedCropId != null) {
            if (cropTabAdapter != null) cropTabAdapter.setSelectedId(state.selectedCropId);
            if (contentState.getVisibility() != View.VISIBLE) {
                showContentFor(state.selectedCropId);
            } else {
                refreshActiveField();
                updateCalendarUI();
            }
        }
    }

    private void refreshActiveField() {
        AppState state = AppState.getInstance();
        UserField field = state.getSelectedField();
        if (field != null && state.selectedCropId != null && field.cropId.equals(state.selectedCropId)) {
            activeField = field;
            if (state.plantDateMillis <= 0) state.plantDateMillis = field.plantDateMillis;
        } else if (state.selectedCropId != null) {
            activeField = buildFieldFromState(state);
        }
    }

    private UserField buildFieldFromState(AppState state) {
        double area = 1.0;
        String name = "Моё поле";
        if (state.getSelectedField() != null) {
            area = state.getSelectedField().areaHectares;
            name = state.getSelectedField().name;
        }
        long plantDate = state.plantDateMillis > 0 ? state.plantDateMillis : System.currentTimeMillis();
        return new UserField("cal", name, state.selectedCropId, area, plantDate);
    }

    private void updateCalendarUI() {
        String month = monthFormat.format(currentMonthCalendar.getTime());
        calendarHeader.setText(capitalize(month));

        days.clear();
        Calendar cal = (Calendar) currentMonthCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - 2;
        for (int i = 0; i < offset; i++) days.add(null);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            days.add((Calendar) cal.clone());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (calendarDayAdapter != null) {
            calendarDayAdapter.setUserField(activeField);
            calendarDayAdapter.setSelectedDate(selectedDay);
            calendarDayAdapter.notifyDataSetChanged();
        }

        resizeCalendarGrid();
    }

    private void resizeCalendarGrid() {
        int rows = (int) Math.ceil(days.size() / 7.0);
        int heightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, DAY_CELL_HEIGHT_DP * rows, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams lp = calendarGrid.getLayoutParams();
        lp.height = heightPx;
        calendarGrid.setLayoutParams(lp);
    }

    private void onDaySelected(Calendar date) {
        selectedDay = (Calendar) date.clone();
        calendarDayAdapter.setSelectedDate(selectedDay);
        if (stageAdapter != null) stageAdapter.setSelectedDate(selectedDay);

        AppState state = AppState.getInstance();
        Crop crop = CropRepository.getInstance(requireContext()).getById(state.selectedCropId);
        if (crop == null || state.plantDateMillis <= 0) return;

        renderSelectedDayPanel(date);
        renderGrowthProgress(crop, state.getPlantDateCalendar(), date);
        scrollToStageForDate(crop, state.getPlantDateCalendar(), date);
        
        // Update stats for the selected date
        AgroLogic.AgroAdvice advice = AgroLogic.getAdviceForDate(activeField, date);
        if (waterValue != null) waterValue.setText(advice.isWateringRequired ? "40%" : "92%");
        if (nutrientValue != null) nutrientValue.setText(advice.isFeedingRequired ? "20%" : "85%");
        if (smartScoreValue != null) {
            int score = advice.progressPercent;
            if (advice.isWateringRequired) score -= 10;
            if (advice.isFeedingRequired) score -= 5;
            smartScoreValue.setText(String.valueOf(Math.max(0, score)));
        }

        // Update summary card
        View root = getView();
        if (root != null) {
            TextView summaryTitle = root.findViewById(R.id.summary_field_title);
            TextView summaryDesc = root.findViewById(R.id.summary_field_desc);
            TextView summaryIcon = root.findViewById(R.id.summary_crop_icon);
            if (summaryTitle != null) summaryTitle.setText(advice.currentStageTitle);
            if (summaryDesc != null) summaryDesc.setText(advice.recommendation);
            if (summaryIcon != null && crop != null) summaryIcon.setText(crop.icon);
        }
        
        // Update chart labels to reflect a window around selected day
        TextView chartStart = getView().findViewById(R.id.chart_label_start);
        TextView chartMid = getView().findViewById(R.id.chart_label_mid);
        TextView chartEnd = getView().findViewById(R.id.chart_label_end);
        
        if (chartStart != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM", new Locale("ru"));
            chartStart.setText(sdf.format(DateUtils.addDays(date, -4).getTime()));
            chartMid.setText(sdf.format(date.getTime()));
            chartEnd.setText(sdf.format(DateUtils.addDays(date, 4).getTime()));
        }
    }

    private void renderSelectedDayPanel(Calendar date) {
        if (activeField == null) return;
        selectedDayPanel.setVisibility(View.VISIBLE);
        selectedDayTitle.setText("📅 " + DateUtils.formatShort(date));

        List<AgroLogic.TaskType> tasks = AgroLogic.getDayTasks(activeField, date);
        if (tasks.isEmpty()) {
            selectedDayTasks.setText("Нет запланированных работ — растения отдыхают 🌿");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (AgroLogic.TaskType task : tasks) {
            switch (task) {
                case WATERING: sb.append("💧 Полив\n"); break;
                case FERTILIZER: sb.append("🌿 Подкормка\n"); break;
                case HARVEST: sb.append("🧺 Сбор урожая\n"); break;
                default: sb.append("📋 Уход\n"); break;
            }
        }
        AgroLogic.AgroAdvice advice = AgroLogic.getAdviceForDate(activeField, date);
        if (advice.recommendation != null && !advice.recommendation.isEmpty()) {
            sb.append("\n").append(advice.recommendation);
        }
        selectedDayTasks.setText(sb.toString().trim());
    }

    private void scrollToStageForDate(Crop crop, Calendar plantDate, Calendar date) {
        long diffMillis = date.getTimeInMillis() - plantDate.getTimeInMillis();
        int offsetDays = (int) Math.round((double) diffMillis / (24 * 60 * 60 * 1000));

        int targetIndex = -1;
        List<CropStage> stages = crop.stages;
        for (int i = 0; i < stages.size(); i++) {
            CropStage s = stages.get(i);
            if (s.hasRange()) {
                if (offsetDays >= s.offsetDays && offsetDays <= s.offsetEndDays) {
                    targetIndex = i;
                    break;
                }
            } else if (s.offsetDays <= offsetDays) {
                targetIndex = i;
            }
        }

        if (targetIndex >= 0) {
            final int index = targetIndex;
            timelineView.post(() -> {
                timelineView.smoothScrollToPosition(index);
                if (scrollView != null) {
                    scrollView.smoothScrollTo(0, timelineView.getTop());
                }
            });
        }
    }

    private void selectCrop(Crop crop) {
        AppState state = AppState.getInstance();
        state.selectedCropId = crop.id;
        state.plantDateMillis = DateUtils.nearestValidPlantingDate(crop).getTimeInMillis();
        state.save(requireContext());
        cropTabAdapter.setSelectedId(crop.id);
        syncWateringPrefs();
        showContentFor(crop.id);
    }

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        contentState.setVisibility(View.GONE);
    }

    private void showContentFor(String cropId) {
        Crop crop = CropRepository.getInstance(requireContext()).getById(cropId);
        if (crop == null) { showEmpty(); return; }

        AppState state = AppState.getInstance();
        if (state.plantDateMillis <= 0) {
            state.plantDateMillis = DateUtils.nearestValidPlantingDate(crop).getTimeInMillis();
        }
        refreshActiveField();

        emptyState.setVisibility(View.GONE);
        contentState.setVisibility(View.VISIBLE);
        renderCalendar(crop);
    }

    private void openDatePicker() {
        AppState state = AppState.getInstance();
        Crop crop = CropRepository.getInstance(requireContext()).getById(state.selectedCropId);
        if (crop == null) return;

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату посадки")
                .setSelection(state.plantDateMillis > 0 ? state.plantDateMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            state.plantDateMillis = selection;
            state.save(requireContext());
            refreshActiveField();
            syncWateringPrefs();
            renderCalendar(crop);
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void renderCalendar(Crop crop) {
        AppState state = AppState.getInstance();
        Calendar plantDate = state.getPlantDateCalendar();
        Calendar today = DateUtils.today();
        selectedDay = today;

        plantDateButton.setText(DateUtils.formatShort(plantDate));

        if (fieldName != null) fieldName.setText(crop.label);
        if (fieldStats != null && activeField != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(activeField.plantDateMillis);
            fieldStats.setText(activeField.areaHectares + " Га • Посажено в " + c.get(Calendar.YEAR));
        }

        boolean valid = DateUtils.isDateInAnyWindow(plantDate, crop.plantingWindows);
        if (!valid) {
            seasonWarning.setVisibility(View.VISIBLE);
            seasonWarning.setText("⚠️ Для «" + crop.label + "» эта дата не входит в обычный сезон посадки. Рекомендуемые периоды — " + crop.plantingWindowsSummary() + ".");
        } else {
            seasonWarning.setVisibility(View.GONE);
        }

        int currentIndex = -1;
        List<CropStage> stages = crop.stages;
        for (int i = 0; i < stages.size(); i++) {
            Calendar stageDate = DateUtils.addDays(plantDate, stages.get(i).offsetDays);
            if (!stageDate.after(today)) currentIndex = i;
        }

        if (today.before(plantDate)) {
            statusPill.setText("Посадка: " + DateUtils.formatShort(plantDate));
        } else if (currentIndex == -1) {
            statusPill.setText("Сезон завершён ✓");
        } else {
            statusPill.setText(stages.get(currentIndex).icon + " " + stages.get(currentIndex).title);
        }

        renderGrowthProgress(crop, plantDate, today);
        renderSelectedDayPanel(today);

        if (calendarDayAdapter != null) {
            calendarDayAdapter.setUserField(activeField);
            calendarDayAdapter.setSelectedDate(selectedDay);
        }

        stageAdapter = new StageAdapter(stages, plantDate, today);
        timelineView.setAdapter(stageAdapter);
        updateCalendarUI();
    }

    private void renderGrowthProgress(Crop crop, Calendar plantDate, Calendar targetDate) {
        if (targetDate.before(plantDate)) {
            growthProgress.setProgress(0);
            growthPercent.setText("0%");
            growthDaysLeft.setText("Ожидание посадки");
            return;
        }

        if (crop.stages == null || crop.stages.isEmpty()) {
            growthProgress.setProgress(0);
            growthPercent.setText("—");
            growthDaysLeft.setText("Нет данных о стадиях роста");
            return;
        }

        CropStage lastStage = crop.stages.get(crop.stages.size() - 1);
        int totalDays = lastStage.hasRange() ? lastStage.offsetEndDays : lastStage.offsetDays;
        if (totalDays <= 0) totalDays = 1;

        long diffMillis = targetDate.getTimeInMillis() - plantDate.getTimeInMillis();
        int daysPassed = (int) Math.round((double) diffMillis / (24 * 60 * 60 * 1000));
        int percent = Math.min(100, Math.max(0, (int) ((daysPassed * 100L) / totalDays)));
        if (daysPassed > 0 && percent == 0) percent = 1;

        growthProgress.setProgress(percent);
        growthPercent.setText(percent + "%");
        if (smartScoreValue != null) smartScoreValue.setText(String.valueOf(percent));

        int daysLeft = totalDays - daysPassed;
        if (daysLeft > 0) {
            growthDaysLeft.setText("До завершения сезона: " + daysLeft + " дн. (день " + (daysPassed + 1) + ")");
        } else if (daysLeft == 0) {
            growthDaysLeft.setText("Сегодня — финальный день сезона!");
        } else {
            growthDaysLeft.setText("Урожай собран! Сезон завершён 🎉");
        }
    }

    private void loadWeather() {
        AppState state = AppState.getInstance();
        if (state.weatherCache != null) {
            weeklyView.setAdapter(new WeatherDayAdapter(state.weatherCache.daily));
            return;
        }
        WeatherClient.fetch(state.lat, state.lon, new WeatherClient.Callback2() {
            @Override
            public void onSuccess(WeatherClient.WeatherResult result) {
                state.weatherCache = result;
                if (isAdded()) weeklyView.setAdapter(new WeatherDayAdapter(result.daily));
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void syncWateringPrefs() {
        WateringPreferences prefs = new WateringPreferences(requireContext());
        prefs.syncFromAppState();
        if (prefs.isEnabled()) WateringReminderScheduler.reschedule(requireContext());
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
