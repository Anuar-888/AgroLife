package kz.spelost.agroapp.util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.network.WeatherClient;

/**
 * Расчёт норм полива и интервалов напоминаний.
 * Нормы л/м² — ориентировочные для периода активного роста (агрономические справочники).
 */
public final class WateringCalculator {

    public enum WateringLevel { NONE, LOW, MEDIUM, HIGH }

    /** Расход воды на один полив, л/м². */
    private static final Map<String, Integer> WATER_RATES = new HashMap<>();
    static {
        WATER_RATES.put("raspberry", 15);
        WATER_RATES.put("strawberry", 10);
        WATER_RATES.put("garlic", 8);
    }

    private static final int DEFAULT_RATE = 10;

    private WateringCalculator() {}

    public static int getRatePerSqm(String cropId) {
        return WATER_RATES.getOrDefault(cropId, DEFAULT_RATE);
    }

    public static long litersForArea(String cropId, double areaSqm) {
        return Math.round(getRatePerSqm(cropId) * areaSqm);
    }

    /** Текущий этап — последний, чья дата начала не позже сегодня. */
    public static CropStage currentStage(Crop crop, Calendar plantDate) {
        Calendar today = DateUtils.today();
        CropStage current = null;
        for (CropStage stage : crop.stages) {
            Calendar stageDate = DateUtils.addDays(plantDate, stage.offsetDays);
            if (!stageDate.after(today)) {
                current = stage;
            }
        }
        return current;
    }

    /** Уровень полива по тексту задачи этапа. */
    public static WateringLevel levelForStage(CropStage stage) {
        if (stage == null) return WateringLevel.NONE;
        String task = stage.task.toLowerCase(Locale.ROOT);
        if (task.contains("сократить полив") || task.contains("прекратить")) {
            return WateringLevel.NONE;
        }
        if (task.contains("умеренный полив")) {
            return WateringLevel.LOW;
        }
        if (task.contains("стабильный полив") || task.contains("регулярный полив")) {
            return WateringLevel.HIGH;
        }
        if (task.contains("полив") || task.contains("влажност")) {
            return WateringLevel.MEDIUM;
        }
        return WateringLevel.MEDIUM;
    }

    public static int intervalDays(WateringLevel level, WeatherClient.WeatherResult weather) {
        if (level == WateringLevel.NONE) return -1;
        int days;
        switch (level) {
            case HIGH: days = 3; break;
            case MEDIUM: days = 5; break;
            case LOW: default: days = 7; break;
        }
        if (weather != null && isHotAndDry(weather)) {
            days = Math.max(2, days - 1);
        }
        return days;
    }

    public static boolean isRainExpected(WeatherClient.WeatherResult weather) {
        if (weather == null || weather.daily.isEmpty()) return false;
        for (int i = 0; i < Math.min(2, weather.daily.size()); i++) {
            if (isRainCode(weather.daily.get(i).weatherCode)) return true;
        }
        return false;
    }

    private static boolean isRainCode(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || (code >= 95 && code <= 99);
    }

    private static boolean isHotAndDry(WeatherClient.WeatherResult weather) {
        if (weather == null || weather.daily.isEmpty()) return false;
        WeatherClient.DayForecast today = weather.daily.get(0);
        return today.tempMax >= 28 && !isRainCode(today.weatherCode);
    }

    public static Calendar nextWateringDate(Calendar from, int intervalDays, WeatherClient.WeatherResult weather) {
        if (intervalDays <= 0) return null;
        Calendar next = DateUtils.addDays(from, intervalDays);
        if (isRainExpected(weather)) {
            next = DateUtils.addDays(from, 1);
        }
        return next;
    }

    public static String formatAdvice(Crop crop, CropStage stage, double areaSqm, WeatherClient.WeatherResult weather) {
        WateringLevel level = levelForStage(stage);
        if (stage == null) {
            return "Сезон завершён — полив не требуется.";
        }
        if (level == WateringLevel.NONE) {
            return "На этапе «" + stage.title + "» полив сокращён или не требуется.";
        }
        int rate = getRatePerSqm(crop.id);
        long liters = litersForArea(crop.id, areaSqm);
        int interval = intervalDays(level, weather);
        String freq;
        switch (level) {
            case HIGH: freq = "каждые 2–3 дня"; break;
            case MEDIUM: freq = "раз в 4–5 дней"; break;
            default: freq = "раз в неделю"; break;
        }
        String rainNote = isRainExpected(weather) ? " Сегодня/завтра возможен дождь — полив можно отложить." : "";
        return String.format(Locale.getDefault(),
                "≈ %d л на полив (норма %d л/м²). Рекомендуем %s.%s",
                liters, rate, freq, rainNote);
    }
}
