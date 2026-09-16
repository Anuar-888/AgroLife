package kz.spelost.agroapp.model;

public class CropCategory {
    public final String id;
    public final String label;
    public final String emoji;
    public final int accentColor;
    public final int bannerRes;

    public CropCategory(String id, String label, String emoji, int accentColor, int bannerRes) {
        this.id = id;
        this.label = label;
        this.emoji = emoji;
        this.accentColor = accentColor;
        this.bannerRes = bannerRes;
    }
}
