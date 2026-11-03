package tidypanels.submarkets;

public enum ButtonLayout {

    TILES("Tiles"),
    STRETCHED("Stretched"),
    FIVE_PER_ROW("Five per row");

    // defaultValue in data/config/LunaSettings.csv; a saved 1.0.a "Unfold on hover" falls back to it
    public static final ButtonLayout DEFAULT = TILES;

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
