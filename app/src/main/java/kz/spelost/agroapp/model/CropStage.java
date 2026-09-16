package kz.spelost.agroapp.model;

/**
 * Один этап ухода за культурой.
 * offsetDays / offsetEndDays — смещение в днях от даты посадки.
 * offsetEndDays == -1 значит, что у этапа нет диапазона (одна дата).
 */
public class CropStage {
    public final int offsetDays;
    public final int offsetEndDays; // -1 если нет диапазона
    public final String icon;
    public final String title;
    public final String task;
    public final String fertilizer; // "—" если подкормка не требуется
    public final String description; // Дополнительное описание
    public final String importance; // Важность этапа (Низкая, Средняя, Высокая)

    public CropStage(int offsetDays, int offsetEndDays, String icon, String title, String task, String fertilizer) {
        this(offsetDays, offsetEndDays, icon, title, task, fertilizer, "", "Средняя");
    }

    public CropStage(int offsetDays, int offsetEndDays, String icon, String title, String task, String fertilizer, String description, String importance) {
        this.offsetDays = offsetDays;
        this.offsetEndDays = offsetEndDays;
        this.icon = icon;
        this.title = title;
        this.task = task;
        this.fertilizer = fertilizer;
        this.description = description;
        this.importance = importance;
    }

    public boolean hasRange() {
        return offsetEndDays >= 0;
    }

    public boolean hasFertilizer() {
        return fertilizer != null && !fertilizer.equals("—");
    }
}
