package kz.spelost.agroapp.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.data.WateringPreferences;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.network.WeatherClient;
import kz.spelost.agroapp.util.DateUtils;
import kz.spelost.agroapp.util.WateringCalculator;

/**
 * Планирует одно следующее напоминание о поливе через AlarmManager.
 * После срабатывания Receiver перепланирует следующее.
 */
public final class WateringReminderScheduler {

    public static final String ACTION_WATERING_ALARM = "kz.spelost.agroapp.WATERING_ALARM";

    private WateringReminderScheduler() {}

    public static void reschedule(Context context) {
        cancel(context);
        WateringPreferences prefs = new WateringPreferences(context);
        if (!prefs.isEnabled()) return;

        String cropId = prefs.getCropId();
        if (cropId == null) return;

        Crop crop = CropRepository.getInstance().getById(cropId);
        if (crop == null) return;

        float area = prefs.getAreaSqm();
        if (area <= 0) return;

        Calendar plantDate = Calendar.getInstance();
        long plantMillis = prefs.getPlantDateMillis();
        if (plantMillis > 0) {
            plantDate.setTimeInMillis(plantMillis);
        } else {
            plantDate = DateUtils.nearestValidPlantingDate(crop);
        }
        plantDate.set(Calendar.HOUR_OF_DAY, 0);
        plantDate.set(Calendar.MINUTE, 0);
        plantDate.set(Calendar.SECOND, 0);
        plantDate.set(Calendar.MILLISECOND, 0);

        CropStage stage = WateringCalculator.currentStage(crop, plantDate);
        WateringCalculator.WateringLevel level = WateringCalculator.levelForStage(stage);
        if (level == WateringCalculator.WateringLevel.NONE) return;

        WeatherClient.WeatherResult weather = kz.spelost.agroapp.data.AppState.getInstance().weatherCache;
        int interval = WateringCalculator.intervalDays(level, weather);

        Calendar from = DateUtils.today();
        long lastWatering = prefs.getLastWateringMillis();
        if (lastWatering > 0) {
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(lastWatering);
            last.set(Calendar.HOUR_OF_DAY, 0);
            last.set(Calendar.MINUTE, 0);
            last.set(Calendar.SECOND, 0);
            last.set(Calendar.MILLISECOND, 0);
            from = last;
        }

        Calendar nextDate = WateringCalculator.nextWateringDate(from, interval, weather);
        if (nextDate == null) return;

        Calendar trigger = (Calendar) nextDate.clone();
        trigger.set(Calendar.HOUR_OF_DAY, prefs.getReminderHour());
        trigger.set(Calendar.MINUTE, prefs.getReminderMinute());
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        if (!trigger.after(now)) {
            trigger.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleAlarm(context, trigger.getTimeInMillis());
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(buildPendingIntent(context));
    }

    private static void scheduleAlarm(Context context, long triggerAtMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, WateringNotificationReceiver.class);
        intent.setAction(ACTION_WATERING_ALARM);
        return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /** Текст для UI: когда следующий полив. */
    public static String nextWateringLabel(Context context) {
        WateringPreferences prefs = new WateringPreferences(context);
        if (!prefs.isEnabled()) return null;

        String cropId = prefs.getCropId();
        if (cropId == null || prefs.getAreaSqm() <= 0) return null;

        Crop crop = CropRepository.getInstance().getById(cropId);
        if (crop == null) return null;

        Calendar plantDate = Calendar.getInstance();
        if (prefs.getPlantDateMillis() > 0) {
            plantDate.setTimeInMillis(prefs.getPlantDateMillis());
        }
        plantDate.set(Calendar.HOUR_OF_DAY, 0);
        plantDate.set(Calendar.MINUTE, 0);
        plantDate.set(Calendar.SECOND, 0);
        plantDate.set(Calendar.MILLISECOND, 0);

        CropStage stage = WateringCalculator.currentStage(crop, plantDate);
        if (WateringCalculator.levelForStage(stage) == WateringCalculator.WateringLevel.NONE) {
            return "На текущем этапе полив не требуется.";
        }

        WeatherClient.WeatherResult weather = kz.spelost.agroapp.data.AppState.getInstance().weatherCache;
        int interval = WateringCalculator.intervalDays(
                WateringCalculator.levelForStage(stage), weather);

        Calendar from = DateUtils.today();
        if (prefs.getLastWateringMillis() > 0) {
            from.setTimeInMillis(prefs.getLastWateringMillis());
            from.set(Calendar.HOUR_OF_DAY, 0);
            from.set(Calendar.MINUTE, 0);
            from.set(Calendar.SECOND, 0);
            from.set(Calendar.MILLISECOND, 0);
        }

        Calendar next = WateringCalculator.nextWateringDate(from, interval, weather);
        if (next == null) return null;

        String time = String.format(Locale.getDefault(), "%02d:%02d",
                prefs.getReminderHour(), prefs.getReminderMinute());
        return String.format(Locale.getDefault(),
                "Следующее напоминание: %s в %s", DateUtils.formatShort(next), time);
    }
}
