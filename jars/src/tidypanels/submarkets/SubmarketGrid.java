package tidypanels.submarkets;

import java.util.function.ToDoubleFunction;

public final class SubmarketGrid {

    // 0.98a-RC8 vanilla strip spacing: 5 px between buttons, 2 px between rows
    public static final float GAP = 5f;
    public static final float ROW_GAP = 2f;
    // 0.98a-RC8 button art: icon inset 4 px at 640:400, name 10 px right of the icon
    public static final float BORDER = 4f;
    public static final float LABEL_PAD = 10f;
    public static final int FIVE = 5;
    public static final String ELLIPSIS = "...";

    public final float width;
    public final float buttonHeight;
    public final int columns;
    public final int count;
    public final int storage;
    // unfold only: a folded tile's width, positions then come from UnfoldRows
    public final float foldedWidth;

    private SubmarketGrid(float width, float buttonHeight, int columns, int count, int storage, float foldedWidth) {
        this.width = width;
        this.buttonHeight = buttonHeight;
        this.columns = Math.max(1, columns);
        this.count = count;
        this.storage = storage;
        this.foldedWidth = foldedWidth;
    }

    public static SubmarketGrid of(ButtonLayout layout, float width, float buttonHeight, float vanillaWidth,
                                   int count, int storage) {
        return switch (layout) {
            case STRETCHED -> new SubmarketGrid(width, buttonHeight, vanillaColumns(width, vanillaWidth),
                    count, storage, 0f);
            case FIVE_PER_ROW -> new SubmarketGrid(width, buttonHeight, FIVE, count, storage, 0f);
            // folded icon tiles plus one tile opened to vanillaWidth, positions come from UnfoldRows
            case TILES -> new SubmarketGrid(width, buttonHeight,
                    unfoldColumns(width, buttonHeight, Math.max(vanillaWidth, iconOnlyWidth(buttonHeight))),
                    count, storage, iconOnlyWidth(buttonHeight));
        };
    }

    // vanilla wraps once x + width + gap passes the strip width
    public static int vanillaColumns(float width, float buttonWidth) {
        return Math.max(1, (int) (width / (buttonWidth + GAP)));
    }

    public static int unfoldColumns(float width, float buttonHeight, float openWidth) {
        float folded = iconOnlyWidth(buttonHeight);
        return Math.max(1, (int) ((width + GAP - (openWidth - folded)) / (folded + GAP)));
    }

    public static float iconWidth(float buttonHeight) {
        return (buttonHeight - 2 * BORDER) * 640f / 400f;
    }

    // folded unfold tile: icon 4 px from both edges; the highlight strip goes to -1 px here, ButtonHighlightPatch skips it
    public static float iconOnlyWidth(float buttonHeight) {
        return iconWidth(buttonHeight) + 2 * BORDER;
    }

    public int slot(int index) {
        if (index == storage) return (rows() - 1) * columns + columns - 1;
        return storage >= 0 && index > storage ? index - 1 : index;
    }

    public int rows() {
        return Math.max(1, (count + columns - 1) / columns);
    }

    public float height() {
        return rows() * (buttonHeight + ROW_GAP);
    }

    public float left(int slot) {
        return edge(slot % columns);
    }

    public float top(int slot) {
        return (slot / columns) * (buttonHeight + ROW_GAP);
    }

    public float buttonWidth(int slot) {
        int column = slot % columns;
        return edge(column + 1) - GAP - edge(column);
    }

    public float labelWidth(int slot) {
        return buttonWidth(slot) - labelInset(buttonHeight);
    }

    // button width taken by everything but the name: both borders, the icon and the gap after it
    public static float labelInset(float buttonHeight) {
        return 2 * BORDER + iconWidth(buttonHeight) + LABEL_PAD;
    }


    // cuts each line on its own, vanilla names carry a line break
    public static String fit(String text, float maxWidth, ToDoubleFunction<String> measure) {
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = fitLine(lines[i], maxWidth, measure);
        }
        return String.join("\n", lines);
    }

    private static String fitLine(String line, float maxWidth, ToDoubleFunction<String> measure) {
        if (measure.applyAsDouble(line) <= maxWidth) return line;
        for (int end = line.length() - 1; end > 0; end--) {
            String cut = line.substring(0, end).stripTrailing() + ELLIPSIS;
            if (measure.applyAsDouble(cut) <= maxWidth) return cut;
        }
        return measure.applyAsDouble(ELLIPSIS) <= maxWidth ? ELLIPSIS : "";
    }

    // rounded column edges, so the last column ends on the strip edge
    private float edge(int column) {
        return Math.round(column * (width + GAP) / columns);
    }
}
