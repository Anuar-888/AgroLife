package kz.spelost.agroapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crops")
public class CropEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String label;
    public String icon;
    public String categoryId;
    
    // JSON strings for complex nested objects for simplicity in this prototype
    public String plantingWindowsJson;
    public String stagesJson;

    public CropEntity(@NonNull String id, String label, String icon, String categoryId, 
                      String plantingWindowsJson, String stagesJson) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.categoryId = categoryId;
        this.plantingWindowsJson = plantingWindowsJson;
        this.stagesJson = stagesJson;
    }
}
