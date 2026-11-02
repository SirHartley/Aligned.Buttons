package alignedbuttons.submarkets;

public final class SubmarketGrid {

    // 0.98a-RC8 vanilla strip spacing: 5 px between buttons, 2 px between rows
    public static final float GAP = 5f;
    public static final float ROW_GAP = 2f;

    public final float width;
    public final float buttonHeight;
    public final int columns;
    public final int count;
    public final int storage;

    private SubmarketGrid(float width, float buttonHeight, int columns, int count, int storage) {
        this.width = width;
        this.buttonHeight = buttonHeight;
        this.columns = Math.max(1, columns);
        this.count = count;
        this.storage = storage;
    }

    public static SubmarketGrid of(float width, float buttonHeight, float vanillaWidth, int count, int storage) {
        return new SubmarketGrid(width, buttonHeight, vanillaColumns(width, vanillaWidth), count, storage);
    }

    // vanilla wraps once x + width + gap passes the strip width
    public static int vanillaColumns(float width, float buttonWidth) {
        return Math.max(1, (int) (width / (buttonWidth + GAP)));
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

    // rounded column edges, so the last column ends on the strip edge
    private float edge(int column) {
        return Math.round(column * (width + GAP) / columns);
    }
}
