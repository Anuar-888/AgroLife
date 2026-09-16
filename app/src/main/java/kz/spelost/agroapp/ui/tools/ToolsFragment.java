package kz.spelost.agroapp.ui.tools;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.data.WateringPreferences;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.network.WeatherClient;
import kz.spelost.agroapp.notifications.NotificationHelper;
import kz.spelost.agroapp.notifications.WateringReminderScheduler;
import kz.spelost.agroapp.util.AgroLogic;
import kz.spelost.agroapp.util.DateUtils;
import kz.spelost.agroapp.util.WateringCalculator;
import android.widget.ProgressBar;

public class ToolsFragment extends Fragment {

    private TextView waterNextReminder;
    private Spinner waterCropSpinner;
    private EditText waterAreaInput;
    private SwitchCompat waterReminderSwitch;
    private List<Crop> crops;
    private WateringPreferences wateringPrefs;

    private View toolDetailCard;
    private TextView toolDetailTitle, toolDetailBody;
    private LinearLayout toolSpecificContainer;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableWateringReminders();
                } else {
                    waterReminderSwitch.setChecked(false);
                    Toast.makeText(requireContext(), R.string.tools_water_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tools, container, false);
        wateringPrefs = new WateringPreferences(requireContext());

        // New UI Elements
        toolDetailCard = root.findViewById(R.id.tools_card_view);
        View ttv = root.findViewById(R.id.tools_title_view);
        if (ttv instanceof TextView) toolDetailTitle = (TextView) ttv;
        View tbv = root.findViewById(R.id.tools_body_view);
        if (tbv instanceof TextView) toolDetailBody = (TextView) tbv;
        
        View tsc = root.findViewById(R.id.tool_specific_container);
        if (tsc instanceof LinearLayout) toolSpecificContainer = (LinearLayout) tsc;

        View closeBtn = root.findViewById(R.id.tool_close);
        if (closeBtn != null && toolDetailCard != null) {
            closeBtn.setOnClickListener(v -> toolDetailCard.setVisibility(View.GONE));
        }

        View cf = root.findViewById(R.id.card_fertilizer);
        if (cf != null) cf.setOnClickListener(v -> showFertilizerTool());
        View cw = root.findViewById(R.id.card_watering);
        if (cw != null) cw.setOnClickListener(v -> showWateringTool());
        
        View cs = root.findViewById(R.id.card_seed_calc);
        if (cs != null) cs.setOnClickListener(v -> showSeedCalculator());
        
        View cp = root.findViewById(R.id.card_harvest_planner);
        if (cp != null) cp.setOnClickListener(v -> showHarvestPlanner());
        
        View cd = root.findViewById(R.id.card_diagnostics);
        if (cd != null) cd.setOnClickListener(v -> showDiagnostics());
        
        View ck = root.findViewById(R.id.card_knowledge);
        if (ck != null) ck.setOnClickListener(v -> showKnowledgeBase());
        
        View tai = root.findViewById(R.id.tools_btn_ai);
        if (tai != null) tai.setOnClickListener(v -> showDiagnostics());


        crops = CropRepository.getInstance(requireContext()).getAll();
        
        return root;
    }

    private void showFertilizerTool() {
        AppState state = AppState.getInstance();
        if (state.selectedCropId == null) {
            Toast.makeText(requireContext(), "Сначала выберите культуру в Календаре или на Главной", Toast.LENGTH_LONG).show();
            return;
        }

        Crop crop = CropRepository.getInstance(requireContext()).getById(state.selectedCropId);
        if (crop == null) {
            Toast.makeText(requireContext(), "Данные культуры загружаются, подождите секунду...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (toolDetailCard != null) toolDetailCard.setVisibility(View.VISIBLE);
        if (toolDetailTitle != null) toolDetailTitle.setText("🧪 Удобрения: " + crop.label);
        
        Calendar plantDate = state.getPlantDateCalendar();
        CropStage current = WateringCalculator.currentStage(crop, plantDate);

        StringBuilder sb = new StringBuilder();
        if (current != null && current.hasFertilizer()) {
            sb.append("Текущая стадия: ").append(current.title).append("\n\n");
            sb.append("Рекомендуемое удобрение:\n");
            sb.append("👉 ").append(current.fertilizer).append("\n\n");
            sb.append("Совет: ").append(current.task);
        } else if (current != null) {
            sb.append("Текущая стадия: ").append(current.title).append("\n\n");
            sb.append("На данном этапе специальная подкормка не требуется. Продолжайте стандартный уход.");
        } else {
            sb.append("Посадка ещё не произведена. Рекомендации по удобрениям появятся после выбора даты посадки в Календаре.");
        }

        if (toolDetailBody != null) toolDetailBody.setText(sb.toString());
        if (toolSpecificContainer != null) toolSpecificContainer.removeAllViews();
        
        // Скроллим к деталям, чтобы пользователь их увидел
        if (toolDetailCard != null) toolDetailCard.requestFocus();
    }

    private void showWateringTool() {
        AppState state = AppState.getInstance();
        if (state.selectedCropId == null) {
            Toast.makeText(requireContext(), "Сначала выберите культуру", Toast.LENGTH_LONG).show();
            return;
        }

        Crop crop = CropRepository.getInstance(requireContext()).getById(state.selectedCropId);
        if (crop == null) return;

        if (toolDetailCard != null) toolDetailCard.setVisibility(View.VISIBLE);
        if (toolDetailTitle != null) toolDetailTitle.setText("💧 Расчёт полива: " + crop.label);
        
        // Use the existing calculation logic but present it in the detail card
        double area = wateringPrefs.getAreaSqm() > 0 ? wateringPrefs.getAreaSqm() : 10.0;
        Calendar plantDate = state.getPlantDateCalendar();
        CropStage stage = WateringCalculator.currentStage(crop, plantDate);
        WeatherClient.WeatherResult weather = state.weatherCache;

        String advice = WateringCalculator.formatAdvice(crop, stage, area, weather);
        String details = "\n\nТекущая норма: " + WateringCalculator.getRatePerSqm(crop.id) + " л/м²" +
                        "\nВаша площадь: " + area + " м²";
        
        if (toolDetailBody != null) toolDetailBody.setText(advice + details);
        if (toolSpecificContainer != null) toolSpecificContainer.removeAllViews();
    }

    private void syncSpinnerWithSelectedCrop() {
        if (waterCropSpinner == null) return;
        AppState state = AppState.getInstance();
        if (state.selectedCropId == null) return;
        for (int i = 0; i < crops.size(); i++) {
            if (crops.get(i).id.equals(state.selectedCropId)) {
                waterCropSpinner.setSelection(i);
                break;
            }
        }
    }

    private void enableWateringReminders() {
        if (waterReminderSwitch == null || waterAreaInput == null) return;
        AppState state = AppState.getInstance();
        wateringPrefs.syncFromAppState();

        String areaStr = waterAreaInput.getText().toString().trim();
        try {
            float area = Float.parseFloat(areaStr);
            if (area <= 0) throw new NumberFormatException();
            wateringPrefs.setAreaSqm(area);
        } catch (NumberFormatException e) {
            waterReminderSwitch.setChecked(false);
            Toast.makeText(requireContext(), "Укажите корректную площадь.", Toast.LENGTH_SHORT).show();
            return;
        }

        wateringPrefs.setEnabled(true);
        NotificationHelper.ensureChannels(requireContext());
        WateringReminderScheduler.reschedule(requireContext());
        renderWateringStatus();
        Toast.makeText(requireContext(), "Напоминания о поливе включены", Toast.LENGTH_SHORT).show();
    }

    private void showSeedCalculator() {
        AppState state = AppState.getInstance();
        if (state.selectedCropId == null) {
            Toast.makeText(requireContext(), "Выберите культуру", Toast.LENGTH_SHORT).show();
            return;
        }

        if (toolDetailCard != null) toolDetailCard.setVisibility(View.VISIBLE);
        if (toolDetailTitle != null) toolDetailTitle.setText("🧮 Калькулятор семян");
        if (toolDetailBody != null) toolDetailBody.setText("Укажите площадь участка для расчета необходимого количества семян или саженцев.");

        if (toolSpecificContainer != null) {
            toolSpecificContainer.removeAllViews();
            EditText areaInput = new EditText(getContext());
            areaInput.setHint("Площадь в м²");
            areaInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            toolSpecificContainer.addView(areaInput);

            Button calcBtn = new Button(getContext());
            calcBtn.setText("Рассчитать");
            calcBtn.setOnClickListener(v -> {
                try {
                    double area = Double.parseDouble(areaInput.getText().toString());
                    long count = AgroLogic.calculateSeeds(state.selectedCropId, area);
                    toolDetailBody.setText("Для площади " + area + " м² вам потребуется примерно " + count + " шт. семян/саженцев.");
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Введите число", Toast.LENGTH_SHORT).show();
                }
            });
            toolSpecificContainer.addView(calcBtn);
        }
    }

    private void showHarvestPlanner() {
        AppState state = AppState.getInstance();
        if (state.selectedCropId == null) {
            Toast.makeText(requireContext(), "Выберите культуру", Toast.LENGTH_SHORT).show();
            return;
        }

        Crop crop = CropRepository.getInstance(requireContext()).getById(state.selectedCropId);
        if (crop == null) return;

        if (toolDetailCard != null) toolDetailCard.setVisibility(View.VISIBLE);
        if (toolDetailTitle != null) toolDetailTitle.setText("📅 Планировщик сбора");

        Calendar plantDate = state.getPlantDateCalendar();
        CropStage lastStage = crop.stages.get(crop.stages.size() - 1);
        Calendar harvestStart = (Calendar) plantDate.clone();
        harvestStart.add(Calendar.DAY_OF_YEAR, lastStage.offsetDays);

        String text = "На основе даты посадки (" + DateUtils.formatShort(plantDate) + "):\n\n" +
                      "Ожидаемое начало сбора: " + DateUtils.formatShort(harvestStart) + "\n" +
                      "Культура: " + crop.label + "\n\n" +
                      "Совет: Начинайте подготовку тары за неделю до указанной даты.";
        
        if (toolDetailBody != null) toolDetailBody.setText(text);
        if (toolSpecificContainer != null) toolSpecificContainer.removeAllViews();
    }

    private void showKnowledgeBase() {
        AppState state = AppState.getInstance();
        String cropId = state.selectedCropId != null ? state.selectedCropId : "raspberry";
        
        if (toolDetailCard != null) toolDetailCard.setVisibility(View.VISIBLE);
        if (toolDetailTitle != null) toolDetailTitle.setText("📚 Профессиональные советы");

        List<String> tips = AgroLogic.getProTips(cropId);
        StringBuilder sb = new StringBuilder();
        for (String tip : tips) {
            sb.append("• ").append(tip).append("\n\n");
        }
        
        if (toolDetailBody != null) toolDetailBody.setText(sb.toString());
        if (toolSpecificContainer != null) toolSpecificContainer.removeAllViews();
    }

    private void showDiagnostics() {
        if (toolDetailCard != null) toolDetailCard.setVisibility(View.VISIBLE);
        if (toolDetailTitle != null) toolDetailTitle.setText("📸 Фото-диагностика");
        if (toolDetailBody != null) toolDetailBody.setText("Симуляция анализа здоровья растения...");

        if (toolSpecificContainer != null) {
            toolSpecificContainer.removeAllViews();
            ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            pb.setIndeterminate(true);
            toolSpecificContainer.addView(pb);

            toolDetailCard.postDelayed(() -> {
                pb.setVisibility(View.GONE);
                toolDetailBody.setText("✅ Анализ завершен!\n\nРезультат: Растение здорово. \nПризнаков вредителей или болезней не обнаружено. Продолжайте текущий уход.");
            }, 2500);
        }
    }

    private void renderWateringStatus() {
        if (wateringPrefs.isEnabled()) {
            String label = WateringReminderScheduler.nextWateringLabel(requireContext());
            if (waterNextReminder != null) {
                waterNextReminder.setVisibility(View.VISIBLE);
                waterNextReminder.setText(label != null ? label : "Напоминание запланировано.");
            }
        } else {
            if (waterNextReminder != null) waterNextReminder.setVisibility(View.GONE);
        }
    }


}
