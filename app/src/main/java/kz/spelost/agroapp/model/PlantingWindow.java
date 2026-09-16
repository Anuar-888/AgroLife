package kz.spelost.agroapp.model;

/**
 * Допустимый сезонный период посадки культуры.
 * Месяцы указываются как 1-12 (человеко-читаемо), не 0-11 как в Calendar.
 */
public class PlantingWindow {
    public final int startMonth;
    public final int startDay;
    public final int endMonth;
    public final int endDay;
    public final String label;

    public PlantingWindow(int startMonth, int startDay, int endMonth, int endDay, String label) {
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.label = label;
    }
}
