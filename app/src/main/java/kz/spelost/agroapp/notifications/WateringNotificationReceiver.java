package kz.spelost.agroapp.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.Locale;

import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.data.WateringPreferences;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.network.WeatherClient;
import kz.spelost.agroapp.util.WateringCalculator;

public class WateringNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!WateringReminderScheduler.ACTION_WATERING_ALARM.equals(intent.getAction())) return;

        WateringPreferences prefs = new WateringPreferences(context);
        if (!prefs.isEnabled()) return;

        String cropId = prefs.getCropId();
        Crop crop = cropId != null ? CropRepository.getInstance().getById(cropId) : null;
        if (crop == null) {
            WateringReminderScheduler.reschedule(context);
            return;
        }

        float area = prefs.getAreaSqm();
        if (area <= 0) {
            WateringReminderScheduler.reschedule(context);
            return;
        }

        Calendar plantDate = Calendar.getInstance();
        if (prefs.getPlantDateMillis() > 0) {
            plantDate.setTimeInMillis(prefs.getPlantDateMillis());
        }

        CropStage stage = WateringCalculator.currentStage(crop, plantDate);
        WeatherClient.WeatherResult weather = kz.spelost.agroapp.data.AppState.getInstance().weatherCache;

        if (WateringCalculator.isRainExpected(weather)) {
            NotificationHelper.showWateringReminder(context,
                    "💧 Полив можно отложить",
                    "Для «" + crop.label + "» ожидаются осадки — полив не нужен.");
            prefs.setLastWateringMillis(System.currentTimeMillis());
            WateringReminderScheduler.reschedule(context);
            return;
        }

        if (WateringCalculator.levelForStage(stage) == WateringCalculator.WateringLevel.NONE) {
            WateringReminderScheduler.cancel(context);
            return;
        }

        long liters = WateringCalculator.litersForArea(crop.id, area);
        String stageTitle = stage != null ? stage.title : "активный рост";
        String body = String.format(Locale.getDefault(),
                "Пора полить «%s» (этап: %s). Норма ≈ %d л на ваш участок (%.0f м²).",
                crop.label, stageTitle, liters, area);

        NotificationHelper.showWateringReminder(context, "💧 Время полива", body);
        prefs.setLastWateringMillis(System.currentTimeMillis());
        WateringReminderScheduler.reschedule(context);
    }
}
