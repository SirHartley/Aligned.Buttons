package alignedbuttons.submarkets;

public enum SelectedHighlight {

    OFF("Off"),
    BRIGHT_BAND("Bright band"),
    ANIMATED("Animated");

    // defaultValue in data/config/LunaSettings.csv
    public static final SelectedHighlight DEFAULT = ANIMATED;

    // radio option text in data/config/LunaSettings.csv, also what LunaLib saves
    public final String label;

    SelectedHighlight(String label) {
        this.label = label;
    }

    public static SelectedHighlight byLabel(String label) {
        for (SelectedHighlight highlight : values()) {
            if (highlight.label.equals(label)) return highlight;
        }
        return DEFAULT;
    }
}
