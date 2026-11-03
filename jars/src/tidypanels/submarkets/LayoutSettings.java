package tidypanels.submarkets;

import lunalib.lunaSettings.LunaSettings;

public final class LayoutSettings {

    public static final String MOD_ID = "tidypanels";
    public static final String LAYOUT = "tidypanels_layout";
    public static final String TILE_UNFOLD = "tidypanels_tile_unfold";
    public static final String UNFOLD_TIME = "tidypanels_unfold_time";
    // seconds; defaultValue, minValue and maxValue in data/config/LunaSettings.csv
    public static final float UNFOLD_TIME_DEFAULT = 0.1f;
    public static final float UNFOLD_TIME_MIN = 0f;
    public static final float UNFOLD_TIME_MAX = 1f;
    public static final String SELECTED_HIGHLIGHT = "tidypanels_selected_highlight";
    public static final String FIX_CORNERS = "tidypanels_fix_corners";
    public static final boolean FIX_CORNERS_DEFAULT = true;

    private static SelectedHighlight selectedHighlight = SelectedHighlight.DEFAULT;
    private static boolean fixCorners = FIX_CORNERS_DEFAULT;

    private LayoutSettings() {
    }

    // read once per strip build, which only happens when a screen opens
    public static ButtonLayout layout() {
        return ButtonLayout.byLabel(LunaSettings.getString(MOD_ID, LAYOUT));
    }

    public static TileUnfold tileUnfold() {
        return TileUnfold.byLabel(LunaSettings.getString(MOD_ID, TILE_UNFOLD));
    }

    public static float unfoldTime() {
        return unfoldTime(LunaSettings.getDouble(MOD_ID, UNFOLD_TIME));
    }

    public static float unfoldTime(Double seconds) {
        if (seconds == null || seconds.isNaN()) return UNFOLD_TIME_DEFAULT;
        return (float) Math.max(UNFOLD_TIME_MIN, Math.min(UNFOLD_TIME_MAX, seconds));
    }

    // the button render patches run every frame, so they read these copies, refreshed when a strip is built
    public static void readRenderSettings() {
        selectedHighlight = SelectedHighlight.byLabel(LunaSettings.getString(MOD_ID, SELECTED_HIGHLIGHT));
        fixCorners = fixCorners(LunaSettings.getBoolean(MOD_ID, FIX_CORNERS));
    }

    public static SelectedHighlight selectedHighlight() {
        return selectedHighlight;
    }

    public static boolean fixCorners() {
        return fixCorners;
    }

    public static boolean fixCorners(Boolean value) {
        return value != null ? value : FIX_CORNERS_DEFAULT;
    }
}
