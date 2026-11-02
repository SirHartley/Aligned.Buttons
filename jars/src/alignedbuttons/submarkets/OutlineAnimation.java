package alignedbuttons.submarkets;

public enum OutlineAnimation {

    OFF("Off"),
    BLACK_BAND("Black band");

    // defaultValue in data/config/LunaSettings.csv
    public static final OutlineAnimation DEFAULT = OFF;

    // radio option text in data/config/LunaSettings.csv, also what LunaLib saves
    public final String label;

    OutlineAnimation(String label) {
        this.label = label;
    }

    public static OutlineAnimation byLabel(String label) {
        for (OutlineAnimation animation : values()) {
            if (animation.label.equals(label)) return animation;
        }
        return DEFAULT;
    }
}
