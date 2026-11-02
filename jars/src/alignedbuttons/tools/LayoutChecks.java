package alignedbuttons.tools;

import alignedbuttons.submarkets.ButtonLayout;
import alignedbuttons.submarkets.SubmarketGrid;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public final class LayoutChecks {

    // 0.98a-RC8: buttons are 48 px high and (int)(48 * 640 / 400) - 2 + 120 = 194 px wide
    private static final float HEIGHT = 48f;
    private static final float VANILLA_WIDTH = 194f;
    private static final float[] WIDTHS = {450f, 700f, 903f, 1000f, 1260f, 1900f};

    private static int checks;

    private LayoutChecks() {
    }

    public static void main(String[] args) throws Exception {
        for (ButtonLayout layout : ButtonLayout.values()) {
            for (float width : WIDTHS) {
                for (int count = 1; count <= 12; count++) {
                    checkGrid(layout, SubmarketGrid.of(layout, width, HEIGHT, VANILLA_WIDTH, count, count - 1));
                    checkGrid(layout, SubmarketGrid.of(layout, width, HEIGHT, VANILLA_WIDTH, count, -1));
                }
            }
        }
        for (float width : WIDTHS) {
            for (int count = 1; count <= 12; count++) checkVanillaRows(width, count);
        }
        require(grid(ButtonLayout.STRETCHED, 903f).columns == 4, "four stretched columns at 903 px");
        require(grid(ButtonLayout.ICONS, 903f).columns == 11, "eleven icon columns at 903 px");
        require(grid(ButtonLayout.FIVE_PER_ROW, 903f).labelWidth(0) == 95f, "95 px for names at 903 px");
        require(SubmarketGrid.iconOnlyWidth(HEIGHT) == 73f, "73 px icon buttons");
        checkFit();
        checkSettings();
        System.out.println("Layout: " + checks + " checks passed");
    }

    private static SubmarketGrid grid(ButtonLayout layout, float width) {
        return SubmarketGrid.of(layout, width, HEIGHT, VANILLA_WIDTH, 5, 4);
    }

    private static void checkGrid(ButtonLayout layout, SubmarketGrid grid) {
        String name = layout + ", " + grid.width + " px, " + grid.count + " buttons, storage " + grid.storage;
        switch (layout) {
            case STRETCHED -> require(grid.buttonWidth(0) >= VANILLA_WIDTH || grid.columns == 1,
                    "never narrower than vanilla: " + name);
            case ICONS -> {
                for (int column = 0; column < grid.columns; column++) {
                    require(grid.buttonWidth(column) == SubmarketGrid.iconOnlyWidth(HEIGHT),
                            "icon sized tiles: " + name);
                }
                float inset = (grid.buttonWidth(0) - SubmarketGrid.iconWidth(HEIGHT)) / 2f;
                require(Math.abs(inset - SubmarketGrid.BORDER) <= 0.5f, "icon centered in its tile: " + name);
                require((grid.columns + 1) * (SubmarketGrid.iconOnlyWidth(HEIGHT) + SubmarketGrid.GAP)
                        > grid.width + SubmarketGrid.GAP, "as many icons as fit: " + name);
            }
            case FIVE_PER_ROW -> require(grid.columns == SubmarketGrid.FIVE, "five columns: " + name);
        }
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
        float firstGap = grid.columns > 1 ? grid.left(1) - grid.buttonWidth(0) : SubmarketGrid.GAP;
        for (int column = 1; column < grid.columns; column++) {
            float gap = grid.left(column) - grid.left(column - 1) - grid.buttonWidth(column - 1);
            if (grid.fixedWidth > 0f) {
                require(gap >= SubmarketGrid.GAP && Math.abs(gap - firstGap) <= 1f, "even gaps: " + name);
            } else {
                require(gap == SubmarketGrid.GAP, "gap between columns: " + name);
                require(Math.abs(grid.buttonWidth(column) - grid.buttonWidth(0)) <= 1f, "even widths: " + name);
            }
        }
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
        SubmarketGrid grid = SubmarketGrid.of(ButtonLayout.STRETCHED, width, HEIGHT, VANILLA_WIDTH, count, count - 1);
        require(grid.rows() == rows + 1, "vanilla row count: " + width + " px, " + count + " buttons");
    }

    private static void checkFit() {
        ToDoubleFunction<String> measure = text -> text.length() * 7.0;
        require(SubmarketGrid.fit("Open\nMarket", 94f, measure).equals("Open\nMarket"), "short names kept");
        require(SubmarketGrid.fit("", 94f, measure).isEmpty(), "empty name kept");
        String cut = SubmarketGrid.fit("Requisitions Centre Annex\nDepot", 94f, measure);
        String[] lines = cut.split("\n", -1);
        require(lines.length == 2 && lines[1].equals("Depot"), "lines cut on their own");
        require(lines[0].endsWith(SubmarketGrid.ELLIPSIS) && measure.applyAsDouble(lines[0]) <= 94f,
                "long line cut to fit: " + lines[0]);
        require(!lines[0].contains(" " + SubmarketGrid.ELLIPSIS), "no space before the dots: " + lines[0]);
        require(SubmarketGrid.fit("Market", 10f, measure).isEmpty(), "nothing fits");
    }

    private static void checkSettings() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("data/config/LunaSettings.csv"));
        require(lines.get(0).equals("fieldID,fieldName,fieldType,defaultValue,secondaryValue,fieldDescription,"
                + "minValue,maxValue,tab"), "luna header");
        List<String> row = null;
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = cells(line);
            require(cells.size() == 9, "nine luna columns: " + cells.get(0));
            if (cells.get(0).equals(ButtonLayout.SETTING)) row = cells;
        }
        require(row != null, "layout setting registered");
        require(row.get(2).equals("Radio"), "layout is a radio");
        require(row.get(3).equals(ButtonLayout.STRETCHED.label), "stretched is the default");
        List<String> options = new ArrayList<>();
        for (String option : row.get(4).split(",")) options.add(option.trim());
        List<String> labels = new ArrayList<>();
        for (ButtonLayout layout : ButtonLayout.values()) labels.add(layout.label);
        require(options.equals(labels), "radio options match ButtonLayout: " + options);
        require(!row.get(5).isBlank(), "layout help text");
        for (ButtonLayout layout : ButtonLayout.values()) {
            require(ButtonLayout.byLabel(layout.label) == layout, "label lookup " + layout);
        }
        require(ButtonLayout.byLabel(null) == ButtonLayout.STRETCHED, "missing value falls back");
        require(ButtonLayout.byLabel("x") == ButtonLayout.STRETCHED, "unknown value falls back");
    }

    // quoted cells only, luna's csv has no escaped quotes
    private static List<String> cells(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
