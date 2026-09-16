package kz.spelost.agroapp.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.MainActivity;
import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.UserField;
import kz.spelost.agroapp.network.WeatherClient;
import kz.spelost.agroapp.util.AgroLogic;

public class HomeFragment extends Fragment {

    private View rootView;
    private RecyclerView cropList;
    private CropChipAdapter adapter;
    private TextView weatherTemp, weatherLoc, weatherDesc;
    private TextView weatherHumidity, weatherPrecip;
    private ViewPager2 fieldPager;
    private TextView adviceTitle, adviceBody;
    private View taskWatering, taskFeeding, taskPruning, tasksContainer;
    private TextView taskPlaceholder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        AppState state = AppState.getInstance();

        fieldPager = rootView.findViewById(R.id.home_viewpager_fields);
        adviceTitle = rootView.findViewById(R.id.home_advice_title);
        adviceBody = rootView.findViewById(R.id.home_advice_body);

        taskWatering = rootView.findViewById(R.id.home_task_watering);
        taskFeeding = rootView.findViewById(R.id.home_task_feeding);
        taskPruning = rootView.findViewById(R.id.home_task_pruning);
        tasksContainer = rootView.findViewById(R.id.home_tasks_container);
        taskPlaceholder = rootView.findViewById(R.id.home_no_tasks_placeholder);

        FieldPagerAdapter fieldAdapter = new FieldPagerAdapter(state.userFields);
        fieldPager.setAdapter(fieldAdapter);
        fieldPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                UserField selected = state.userFields.get(position);
                state.selectedFieldId = selected.id;
                state.selectedCropId = selected.cropId;
                state.plantDateMillis = selected.plantDateMillis;
                state.save(rootView.getContext());
                updateAdvice(selected);
            }
        });
        
        if (!state.userFields.isEmpty()) {
            updateAdvice(state.userFields.get(0));
        }

        rootView.findViewById(R.id.home_btn_profile).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(R.id.nav_profile);
            }
        });

        rootView.findViewById(R.id.home_btn_all_crops).setOnClickListener(v -> {
            AllCropsBottomSheet sheet = new AllCropsBottomSheet();
            sheet.setListener(this::onCropSelected);
            sheet.show(getParentFragmentManager(), "AllCrops");
        });

        cropList = rootView.findViewById(R.id.home_crop_list);
        cropList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        CropRepository.getInstance(requireContext()).getAll(crops -> {
            if (isAdded()) {
                adapter = new CropChipAdapter(crops, this::onCropSelected);
                cropList.setAdapter(adapter);
            }
        });

        EditText search = rootView.findViewById(R.id.home_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        weatherTemp = rootView.findViewById(R.id.home_weather_temp);
        weatherLoc = rootView.findViewById(R.id.home_weather_loc);
        weatherDesc = rootView.findViewById(R.id.home_weather_description);
        weatherHumidity = rootView.findViewById(R.id.home_weather_humidity);
        weatherPrecip = rootView.findViewById(R.id.home_weather_precip);
        weatherLoc.setText(AppState.getInstance().locationName.split(",")[0]);

        rootView.findViewById(R.id.home_btn_add_note).setOnClickListener(v -> {
            EditText input = new EditText(getContext());
            input.setHint("Введите текст заметки...");
            input.setPadding(64, 32, 64, 32);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Новая заметка")
                    .setView(input)
                    .setPositiveButton("Добавить", (dialog, which) -> {
                        String note = input.getText().toString().trim();
                        if (!note.isEmpty()) {
                            android.widget.Toast.makeText(getContext(), "Заметка сохранена", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        View adviceCard = rootView.findViewById(R.id.home_advice_card);
        if (adviceCard != null) {
            adviceCard.setOnClickListener(v -> {
                android.widget.Toast.makeText(getContext(), "Открываем подробный совет...", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        loadWeather();

        return rootView;
    }

    private void updateAdvice(UserField field) {
        AgroLogic.AgroAdvice advice = AgroLogic.getAdvice(field);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d MMMM", new java.util.Locale("ru"));
        adviceTitle.setText(sdf.format(new java.util.Date()));
        adviceBody.setText(advice.organicTip);

        taskWatering.setVisibility(advice.isWateringRequired ? View.VISIBLE : View.GONE);
        taskFeeding.setVisibility(advice.isFeedingRequired ? View.VISIBLE : View.GONE);
        taskPruning.setVisibility(advice.isPruningRequired ? View.VISIBLE : View.GONE);

        boolean hasAnyTask = advice.isWateringRequired || advice.isFeedingRequired || advice.isPruningRequired;
        tasksContainer.setVisibility(hasAnyTask ? View.VISIBLE : View.GONE);
        taskPlaceholder.setVisibility(hasAnyTask ? View.GONE : View.VISIBLE);
        
        // Update task labels based on crop
        TextView wateringText = rootView.findViewById(R.id.home_task_watering_text);
        if (wateringText != null) wateringText.setText("Полив " + field.name);
        
        TextView feedingText = rootView.findViewById(R.id.home_task_feeding_text);
        if (feedingText != null) feedingText.setText("Подкормка " + field.name);
        
        TextView pruningText = rootView.findViewById(R.id.home_task_pruning_text);
        if (pruningText != null) pruningText.setText("Обрезка " + field.name);
    }

    private void filter(String query) {
        CropRepository.getInstance(requireContext()).getAll(all -> {
            if (!isAdded()) return;
            List<Crop> filtered = new ArrayList<>();
            String q = query.trim().toLowerCase(Locale.ROOT);
            for (Crop c : all) {
                if (q.isEmpty() || c.label.toLowerCase(Locale.ROOT).contains(q)) {
                    filtered.add(c);
                }
            }
            cropList.setAdapter(new CropChipAdapter(filtered, this::onCropSelected));
        });
    }

    private void onCropSelected(Crop crop) {
        AppState state = AppState.getInstance();
        state.selectedCropId = crop.id;
        state.plantDateMillis = -1; // рассчитается заново на экране Календаря
        state.save(requireContext());
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(R.id.nav_calendar);
        }
    }

    private void loadWeather() {
        AppState state = AppState.getInstance();
        if (state.weatherCache != null) {
            renderWeather(state.weatherCache);
            return;
        }
        WeatherClient.fetch(state.lat, state.lon, new WeatherClient.Callback2() {
            @Override
            public void onSuccess(WeatherClient.WeatherResult result) {
                state.weatherCache = result;
                if (isAdded()) renderWeather(result);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) weatherDesc.setText("Ошибка загрузки");
            }
        });
    }

    private void renderWeather(WeatherClient.WeatherResult result) {
        weatherTemp.setText(Math.round(result.currentTemp) + "°");
        weatherDesc.setText(WeatherClient.weatherDescription(result.currentWeatherCode));
        if (weatherHumidity != null) weatherHumidity.setText(result.currentHumidity + "%");
        if (weatherPrecip != null) weatherPrecip.setText(result.todayPrecipitationPercent + "%");
    }
}
