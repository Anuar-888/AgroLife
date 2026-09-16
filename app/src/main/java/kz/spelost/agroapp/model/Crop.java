package kz.spelost.agroapp.model;

import java.util.List;

public class Crop {
    public final String id;
    public final String label;
    public final String icon;
    public final String categoryId;
    public final List<PlantingWindow> plantingWindows;
    public final List<CropStage> stages;

    public Crop(String id, String label, String icon, String categoryId,
                List<PlantingWindow> plantingWindows, List<CropStage> stages) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.categoryId = categoryId;
        this.plantingWindows = plantingWindows;
        this.stages = stages;
    }

    /** Совместимость со старым конструктором. */
    public Crop(String id, String label, String icon,
                List<PlantingWindow> plantingWindows, List<CropStage> stages) {
        this(id, label, icon, "vegetables", plantingWindows, stages);
    }

    public String plantingWindowsSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plantingWindows.size(); i++) {
            if (i > 0) sb.append(" или ");
            sb.append(plantingWindows.get(i).label);
        }
        return sb.toString();
    }
}
