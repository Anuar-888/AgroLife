package kz.spelost.agroapp.model;

import java.io.Serializable;

public class UserField implements Serializable {
    public String id;
    public String name; // e.g., "Field Alpha"
    public String cropId;
    public double areaHectares;
    public long plantDateMillis;

    public UserField(String id, String name, String cropId, double areaHectares, long plantDateMillis) {
        this.id = id;
        this.name = name;
        this.cropId = cropId;
        this.areaHectares = areaHectares;
        this.plantDateMillis = plantDateMillis;
    }
}
