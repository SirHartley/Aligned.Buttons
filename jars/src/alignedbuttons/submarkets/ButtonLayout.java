package alignedbuttons.submarkets;

import lunalib.lunaSettings.LunaSettings;

public enum ButtonLayout {

    STRETCHED("Stretched"),
    ICONS("Icons only"),
    FIVE_PER_ROW("Five per row"),
    UNFOLD("Unfold on hover");

    public static final String MOD_ID = "aligned_buttons";
    public static final String SETTING = "aligned_buttons_layout";
    // defaultValue in data/config/LunaSettings.csv
    public static final ButtonLayout DEFAULT = FIVE_PER_ROW;

    // radio option text in data/config/LunaSettings.csv, also what LunaLib saves
    public final String label;

    ButtonLayout(String label) {
        this.label = label;
    }

    // read once per strip build, which only happens when a screen opens
    public static ButtonLayout current() {
        return byLabel(LunaSettings.getString(MOD_ID, SETTING));
    }

    public static ButtonLayout byLabel(String label) {
        for (ButtonLayout layout : values()) {
            if (layout.label.equals(label)) return layout;
        }
        return DEFAULT;
    }
}
