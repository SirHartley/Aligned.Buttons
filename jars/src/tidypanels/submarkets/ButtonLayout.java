package tidypanels.submarkets;

public enum ButtonLayout {

    STRETCHED("Stretched"),
    FIVE_PER_ROW("Five per row"),
    UNFOLD("Unfold on hover");

    // defaultValue in data/config/LunaSettings.csv
    public static final ButtonLayout DEFAULT = UNFOLD;

    // radio option text in data/config/LunaSettings.csv, also what LunaLib saves
    public final String label;

    ButtonLayout(String label) {
        this.label = label;
    }

    public static ButtonLayout byLabel(String label) {
        for (ButtonLayout layout : values()) {
            if (layout.label.equals(label)) return layout;
        }
        return DEFAULT;
    }
}
