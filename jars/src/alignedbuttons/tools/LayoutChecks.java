package alignedbuttons.tools;

import alignedbuttons.submarkets.SubmarketGrid;

public final class LayoutChecks {

    // 0.98a-RC8: buttons are 48 px high and (int)(48 * 640 / 400) - 2 + 120 = 194 px wide
    private static final float HEIGHT = 48f;
    private static final float VANILLA_WIDTH = 194f;
    private static final float[] WIDTHS = {450f, 700f, 903f, 1000f, 1260f, 1900f};

    private static int checks;

    private LayoutChecks() {
    }

    public static void main(String[] args) {
        for (float width : WIDTHS) {
            for (int count = 1; count <= 12; count++) {
                checkGrid(SubmarketGrid.of(width, HEIGHT, VANILLA_WIDTH, count, count - 1));
                checkGrid(SubmarketGrid.of(width, HEIGHT, VANILLA_WIDTH, count, -1));
                checkVanillaRows(width, count);
            }
        }
        require(SubmarketGrid.of(903f, HEIGHT, VANILLA_WIDTH, 5, 4).columns == 4, "four columns at 903 px");
        System.out.println("Layout: " + checks + " checks passed");
    }

    private static void checkGrid(SubmarketGrid grid) {
        String name = grid.width + " px, " + grid.count + " buttons, storage " + grid.storage;
        boolean[] used = new boolean[grid.rows() * grid.columns];
        for (int i = 0; i < grid.count; i++) {
            int slot = grid.slot(i);
            require(slot >= 0 && slot < used.length && !used[slot], "free slot " + i + ": " + name);
            used[slot] = true;
            require(grid.left(slot) >= 0 && grid.left(slot) + grid.buttonWidth(slot) <= grid.width,
                    "inside the strip: " + name);
            require(grid.top(slot) + grid.buttonHeight <= grid.height(), "inside the strip height: " + name);
        }
        if (grid.storage >= 0) {
            require(grid.slot(grid.storage) % grid.columns == grid.columns - 1, "storage in last column: " + name);
            require(grid.slot(grid.storage) / grid.columns == grid.rows() - 1, "storage in last row: " + name);
        }
        int last = grid.columns - 1;
        require(grid.left(last) + grid.buttonWidth(last) == grid.width, "row ends on the strip edge: " + name);
        for (int column = 1; column < grid.columns; column++) {
            require(grid.left(column) - grid.left(column - 1) - grid.buttonWidth(column - 1) == SubmarketGrid.GAP,
                    "gap between columns: " + name);
            require(Math.abs(grid.buttonWidth(column) - grid.buttonWidth(0)) <= 1f, "even widths: " + name);
        }
        require(grid.buttonWidth(0) >= VANILLA_WIDTH || grid.columns == 1, "never narrower than vanilla: " + name);
    }

    // mirrors the vanilla wrap loop, storage counts as a normal button there
    private static void checkVanillaRows(float width, int count) {
        int rows = 0;
        float x = 0f;
        for (int i = 0; i < count; i++) {
            if (x + VANILLA_WIDTH + SubmarketGrid.GAP > width) {
                rows++;
                x = 0f;
            }
            x += VANILLA_WIDTH + SubmarketGrid.GAP;
        }
        SubmarketGrid grid = SubmarketGrid.of(width, HEIGHT, VANILLA_WIDTH, count, count - 1);
        require(grid.rows() == rows + 1, "vanilla row count: " + width + " px, " + count + " buttons");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
