package tidypanels.submarkets;

public enum TileUnfold {

    HOVER("On hover"),
    CLICK("On click"),
    NEVER("Never");

    // defaultValue in data/config/LunaSettings.csv
    public static final TileUnfold DEFAULT = HOVER;

    // radio option text in data/config/LunaSettings.csv, also what LunaLib saves
    public final String label;

    TileUnfold(String label) {
        this.label = label;
    }

    public static TileUnfold byLabel(String label) {
        for (TileUnfold unfold : values()) {
            if (unfold.label.equals(label)) return unfold;
        }
        return DEFAULT;
    }
}
