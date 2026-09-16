package kz.spelost.agroapp.network;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback2 {
        void onSuccess(WeatherResult result);
        void onError(String message);
    }

    public static class DayForecast {
        public String date;
        public double tempMax;
        public double tempMin;
        public int weatherCode;
    }

    public static class WeatherResult {
        public double currentTemp;
        public int currentWeatherCode;
        public List<DayForecast> daily = new ArrayList<>();
        public String sunrise;
        public String sunset;
        public double uvIndexMax;
        public boolean hasFrostWarning;
        public int currentHumidity;
        public int todayPrecipitationPercent;

        public String summaryForChat() {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (DayForecast d : daily) {
                min = Math.min(min, d.tempMin);
                max = Math.max(max, d.tempMax);
            }
            String base = String.format(Locale.getDefault(),
                    "Сейчас %.0f°C, %s. За неделю от %.0f° до %.0f°C.",
                    currentTemp, weatherDescription(currentWeatherCode), min, max);
            if (hasFrostWarning) base += " Ожидаются заморозки.";
            return base;
        }
    }

    public static String weatherDescription(int code) {
        switch (code) {
            case 0: return "Ясно";
            case 1: return "Малооблачно";
            case 2: return "Переменно";
            case 3: return "Пасмурно";
            case 45: case 48: return "Туман";
            case 51: case 53: case 55: return "Морось";
            case 61: case 63: return "Дождь";
            case 65: return "Ливень";
            case 71: case 73: case 75: return "Снег";
            case 80: case 81: case 82: return "Ливень";
            case 95: case 96: case 99: return "Гроза";
            default: return "—";
        }
    }

    public static String weatherEmoji(int code) {
        switch (code) {
            case 0: return "☀️";
            case 1: return "🌤️";
            case 2: return "⛅";
            case 3: return "☁️";
            case 45: case 48: return "🌫️";
            case 51: case 53: case 55: return "🌦️";
            case 61: case 63: case 65: return "🌧️";
            case 71: case 73: case 75: return "🌨️";
            case 80: case 81: case 82: return "🌦️";
            case 95: case 96: case 99: return "⛈️";
            default: return "☀️";
        }
    }

    public static void fetch(double lat, double lon, Callback2 callback) {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                "&current=temperature_2m,weather_code,relative_humidity_2m" +
                "&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset,uv_index_max,precipitation_probability_max" +
                "&timezone=auto&forecast_days=7", lat, lon);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Не удалось загрузить погоду: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Ошибка сервера погоды"));
                        return;
                    }
                    String body = response.body().string();
                    WeatherResult result = parse(body);
                    mainHandler.post(() -> callback.onSuccess(result));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Ошибка разбора данных погоды"));
                } finally {
                    response.close();
                }
            }
        });
    }

    private static WeatherResult parse(String body) throws Exception {
        JSONObject json = new JSONObject(body);
        WeatherResult result = new WeatherResult();

        JSONObject current = json.getJSONObject("current");
        result.currentTemp = current.getDouble("temperature_2m");
        result.currentWeatherCode = current.getInt("weather_code");
        if (current.has("relative_humidity_2m")) {
            result.currentHumidity = current.getInt("relative_humidity_2m");
        }

        JSONObject daily = json.getJSONObject("daily");
        JSONArray time = daily.getJSONArray("time");
        JSONArray tMax = daily.getJSONArray("temperature_2m_max");
        JSONArray tMin = daily.getJSONArray("temperature_2m_min");
        JSONArray wCode = daily.getJSONArray("weather_code");
        JSONArray precip = daily.has("precipitation_probability_max")
                ? daily.getJSONArray("precipitation_probability_max") : null;

        for (int i = 0; i < time.length(); i++) {
            DayForecast d = new DayForecast();
            d.date = time.getString(i);
            d.tempMax = tMax.getDouble(i);
            d.tempMin = tMin.getDouble(i);
            d.weatherCode = wCode.getInt(i);
            if (d.tempMin <= 1) result.hasFrostWarning = true;
            result.daily.add(d);
        }

        if (precip != null && precip.length() > 0) {
            result.todayPrecipitationPercent = precip.getInt(0);
        }

        if (daily.has("sunrise")) result.sunrise = daily.getJSONArray("sunrise").getString(0);
        if (daily.has("sunset")) result.sunset = daily.getJSONArray("sunset").getString(0);
        if (daily.has("uv_index_max")) result.uvIndexMax = daily.getJSONArray("uv_index_max").getDouble(0);

        return result;
    }
}
