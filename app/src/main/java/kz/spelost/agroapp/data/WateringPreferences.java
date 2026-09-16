package kz.spelost.agroapp.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Настройки напоминаний о поливе — сохраняются между сессиями и перезагрузками.
 */
public class WateringPreferences {

    private static final String PREFS = "watering_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_AREA = "area_sqm";
    private static final String KEY_HOUR = "reminder_hour";
    private static final String KEY_MINUTE = "reminder_minute";
    private static final String KEY_CROP_ID = "crop_id";
    private static final String KEY_PLANT_DATE = "plant_date_millis";
    private static final String KEY_LAST_WATERING = "last_watering_millis";

    private final SharedPreferences prefs;

    public WateringPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public float getAreaSqm() {
        return prefs.getFloat(KEY_AREA, 0f);
    }

    public void setAreaSqm(float area) {
        prefs.edit().putFloat(KEY_AREA, area).apply();
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_HOUR, 8);
    }

    public int getReminderMinute() {
        return prefs.getInt(KEY_MINUTE, 0);
    }

    public void setReminderTime(int hour, int minute) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply();
    }

    public String getCropId() {
        return prefs.getString(KEY_CROP_ID, null);
    }

    public void setCropId(String cropId) {
        prefs.edit().putString(KEY_CROP_ID, cropId).apply();
    }

    public long getPlantDateMillis() {
        return prefs.getLong(KEY_PLANT_DATE, -1);
    }

    public void setPlantDateMillis(long millis) {
        prefs.edit().putLong(KEY_PLANT_DATE, millis).apply();
    }

    public long getLastWateringMillis() {
        return prefs.getLong(KEY_LAST_WATERING, -1);
    }

    public void setLastWateringMillis(long millis) {
        prefs.edit().putLong(KEY_LAST_WATERING, millis).apply();
    }

    /** Синхронизировать с AppState при смене культуры/даты. */
    public void syncFromAppState() {
        AppState state = AppState.getInstance();
        if (state.selectedCropId != null) {
            setCropId(state.selectedCropId);
        }
        if (state.plantDateMillis > 0) {
            setPlantDateMillis(state.plantDateMillis);
        }
    }
}
