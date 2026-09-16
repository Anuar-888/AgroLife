package kz.spelost.agroapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import kz.spelost.agroapp.model.UserField;
import kz.spelost.agroapp.network.WeatherClient;

/**
 * Общее состояние экрана между фрагментами.
 * Содержит методы для сохранения и загрузки выбранных данных.
 */
public class AppState {

    private static AppState instance;
    private static final String PREFS_NAME = "spelost_prefs";
    private static final String KEY_SELECTED_CROP = "selected_crop_id";
    private static final String KEY_PLANT_DATE = "plant_date_millis";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_FIELDS = "user_fields";
    private static final String KEY_SELECTED_FIELD = "selected_field_id";

    public String userName = "Anuar";
    public String selectedCropId = null;
    public long plantDateMillis = -1;
    public String selectedFieldId = null;
    public List<UserField> userFields = new ArrayList<>();
    
    public WeatherClient.WeatherResult weatherCache = null;

    public double lat = 43.2220;
    public double lon = 76.8512;
    public String locationName = "Алматы, Казахстан";

    public String testAnthropicApiKey = null;

    private AppState() {}

    public static AppState getInstance() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    public void load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        selectedCropId = prefs.getString(KEY_SELECTED_CROP, null);
        plantDateMillis = prefs.getLong(KEY_PLANT_DATE, -1);
        selectedFieldId = prefs.getString(KEY_SELECTED_FIELD, null);
        userName = prefs.getString(KEY_USER_NAME, "Anuar");
        
        String fieldsJson = prefs.getString(KEY_USER_FIELDS, null);
        if (fieldsJson != null) {
            userFields = new Gson().fromJson(fieldsJson, new TypeToken<List<UserField>>(){}.getType());
        }
        
        // Mock data if empty for demo (Fixed dates: July 15th and August 1st 2026)
        if (userFields.isEmpty()) {
            Calendar c1 = Calendar.getInstance();
            c1.set(2026, Calendar.JULY, 15, 0, 0);
            userFields.add(new UserField("1", "Поле №1 (Малина)", "raspberry", 2.5, c1.getTimeInMillis()));
            
            Calendar c2 = Calendar.getInstance();
            c2.set(2026, Calendar.AUGUST, 1, 0, 0);
            userFields.add(new UserField("2", "Поле №2 (Чеснок)", "garlic", 1.2, c2.getTimeInMillis()));
        }

        if (selectedFieldId == null && !userFields.isEmpty()) {
            selectedFieldId = userFields.get(0).id;
        }
    }

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String fieldsJson = new Gson().toJson(userFields);
        prefs.edit()
                .putString(KEY_SELECTED_CROP, selectedCropId)
                .putLong(KEY_PLANT_DATE, plantDateMillis)
                .putString(KEY_SELECTED_FIELD, selectedFieldId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_USER_FIELDS, fieldsJson)
                .apply();
    }

    public UserField getSelectedField() {
        if (selectedFieldId == null) return null;
        for (UserField f : userFields) {
            if (f.id.equals(selectedFieldId)) return f;
        }
        return null;
    }

    public Calendar getPlantDateCalendar() {
        Calendar c = Calendar.getInstance();
        if (plantDateMillis > 0) {
            c.setTimeInMillis(plantDateMillis);
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
}
