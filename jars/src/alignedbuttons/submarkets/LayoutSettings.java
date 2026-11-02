package alignedbuttons.submarkets;

import lunalib.lunaSettings.LunaSettings;

public final class LayoutSettings {

    public static final String MOD_ID = "aligned_buttons";
    public static final String LAYOUT = "aligned_buttons_layout";
    public static final String UNFOLD_TIME = "aligned_buttons_unfold_time";
    // seconds; defaultValue, minValue and maxValue in data/config/LunaSettings.csv
    public static final float UNFOLD_TIME_DEFAULT = 0.1f;
    public static final float UNFOLD_TIME_MIN = 0f;
    public static final float UNFOLD_TIME_MAX = 1f;

    private LayoutSettings() {
    }

    // read once per strip build, which only happens when a screen opens
    public static ButtonLayout layout() {
        return ButtonLayout.byLabel(LunaSettings.getString(MOD_ID, LAYOUT));
    }

    public static float unfoldTime() {
        return unfoldTime(LunaSettings.getDouble(MOD_ID, UNFOLD_TIME));
    }

    public static float unfoldTime(Double seconds) {
        if (seconds == null || seconds.isNaN()) return UNFOLD_TIME_DEFAULT;
        return (float) Math.max(UNFOLD_TIME_MIN, Math.min(UNFOLD_TIME_MAX, seconds));
    }
}
