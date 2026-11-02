package alignedbuttons.tools;

import alignedbuttons.submarkets.ButtonLayout;
import alignedbuttons.submarkets.LayoutSettings;
import alignedbuttons.submarkets.SubmarketGrid;
import alignedbuttons.submarkets.UnfoldRows;

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
    private static final float[] UNFOLD_TIMES = {0f, LayoutSettings.UNFOLD_TIME_DEFAULT, 0.2f,
            LayoutSettings.UNFOLD_TIME_MAX};

    private static int checks;

    private LayoutChecks() {
    }

    public static void main(String[] args) throws Exception {
        for (ButtonLayout layout : ButtonLayout.values()) {
            if (layout == ButtonLayout.UNFOLD) continue;
            for (float width : WIDTHS) {
                for (int count = 1; count <= 12; count++) {
                    checkGrid(layout, SubmarketGrid.of(layout, width, HEIGHT, VANILLA_WIDTH, count, count - 1));
                    checkGrid(layout, SubmarketGrid.of(layout, width, HEIGHT, VANILLA_WIDTH, count, -1));
                }
            }
        }
        for (float width : WIDTHS) {
            for (int count = 1; count <= 12; count++) {
                checkVanillaRows(width, count);
                for (float time : UNFOLD_TIMES) {
                    checkUnfold(width, count, count - 1, time);
                    checkUnfold(width, count, -1, time);
                }
            }
        }
        require(grid(ButtonLayout.STRETCHED, 903f).columns == 4, "four stretched columns at 903 px");
        require(grid(ButtonLayout.ICONS, 903f).columns == 11, "eleven icon columns at 903 px");
        require(grid(ButtonLayout.UNFOLD, 903f).columns == 10, "ten unfold columns at 903 px");
        require(grid(ButtonLayout.FIVE_PER_ROW, 903f).labelWidth(0) == 95f, "95 px for names at 903 px");
        require(SubmarketGrid.iconOnlyWidth(HEIGHT) == 72f, "72 px icon buttons");
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
                // vanilla draws the icon at x = BORDER, so the right inset must match it exactly
                float right = grid.buttonWidth(0) - SubmarketGrid.BORDER - SubmarketGrid.iconWidth(HEIGHT);
                require(right == SubmarketGrid.BORDER, "icon centered in its tile: " + name);
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

    private static void checkUnfold(float width, int count, int storage, float time) {
        String name = "unfold " + time + " s, " + width + " px, " + count + " buttons, storage " + storage;
        SubmarketGrid grid = SubmarketGrid.of(ButtonLayout.UNFOLD, width, HEIGHT, VANILLA_WIDTH, count, storage);
        float folded = SubmarketGrid.iconOnlyWidth(HEIGHT);
        float step = SubmarketGrid.GAP + folded;
        require((grid.columns - 1) * step + VANILLA_WIDTH <= width, "a full row fits: " + name);
        require(grid.columns * step + VANILLA_WIDTH > width, "as many tiles as fit: " + name);
        UnfoldRows rows = new UnfoldRows(grid, VANILLA_WIDTH, time);
        checkUnfoldRows(rows, name);
        float frame = 1f / 60f;
        int frames = Math.max(1, Math.round(time / frame));
        boolean several = false;
        for (int i = 0; i < count; i++) {
            rows.aim(i);
            for (int f = 0; f < Math.max(1, frames / 4); f++) {
                rows.step(frame);
                checkUnfoldRows(rows, name);
                int moving = 0;
                for (int j = 0; j < count; j++) {
                    if (rows.open(j) > 0f && rows.open(j) < 1f) moving++;
                }
                several |= moving >= 3;
            }
        }
        require(several || time == 0f || grid.columns < 3 || count < 3,
                "a cursor sweep moves several tiles at once: " + name);
        for (int i = 0; i < count; i++) {
            rows.openNow(rows.target(rows.row(i)));
            float x = rows.left(i) + rows.width(i) / 2f;
            float y = rows.top(i) + HEIGHT / 2f;
            boolean wasOpen = rows.open(i) == 1f;
            rows.aim(i);
            for (int f = 0; f < frames; f++) {
                require(rows.at(x, y) == i, "unfolding tile stays under the cursor: " + name);
                rows.step(frame);
                checkUnfoldRows(rows, name);
                if (f < frames - 1 && !wasOpen) require(rows.open(i) < 1f, "not open before the unfold time: " + name);
            }
            require(rows.at(x, y) == i, "open tile still under the cursor: " + name);
            require(rows.open(i) == 1f, "open within the unfold time: " + name);
            require(rows.labelOpacity(i, VANILLA_WIDTH - 40f) == 1f, "open tile shows its name: " + name);
        }
        for (int i = 0; i < count; i++) {
            if (rows.open(i) == 0f) {
                require(rows.labelOpacity(i, SubmarketGrid.labelInset(HEIGHT) + 10f) == 0f,
                        "folded tile hides its name: " + name);
            }
        }
    }

    private static void checkUnfoldRows(UnfoldRows rows, String name) {
        SubmarketGrid grid = rows.grid;
        for (int row = 0; row < rows.rows(); row++) {
            float sum = 0f;
            float total = 0f;
            int size = 0;
            float end = -SubmarketGrid.GAP;
            for (int i = 0; i < grid.count; i++) {
                if (rows.row(i) != row) continue;
                sum += rows.open(i);
                total += rows.width(i);
                size++;
                require(rows.width(i) >= rows.foldedWidth - 0.01f && rows.width(i) <= rows.openWidth + 0.01f,
                        "width between folded and open: " + name);
                require(rows.top(i) == row * (HEIGHT + SubmarketGrid.ROW_GAP), "row top: " + name);
                if (i == grid.storage) {
                    require(rows.left(i) + rows.width(i) == grid.width, "storage on the right edge: " + name);
                } else {
                    require(Math.abs(rows.left(i) - end - SubmarketGrid.GAP) < 0.01f, "5 px gaps: " + name);
                    end = rows.left(i) + rows.width(i);
                }
            }
            if (grid.storage >= 0 && rows.row(grid.storage) == row) {
                require(end + SubmarketGrid.GAP <= rows.left(grid.storage) + 0.01f, "no overlap with storage: " + name);
            }
            require(Math.abs(sum - 1f) < 0.001f, "one tile worth of unfolding per row: " + name);
            float expected = (size - 1) * rows.foldedWidth + rows.openWidth;
            require(Math.abs(total - expected) < 0.05f, "row keeps its width: " + name);
            require(end <= grid.width + 0.01f, "inside the strip: " + name);
        }
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
        List<String> time = null;
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = cells(line);
            require(cells.size() == 9, "nine luna columns: " + cells.get(0));
            if (cells.get(0).equals(LayoutSettings.LAYOUT)) row = cells;
            if (cells.get(0).equals(LayoutSettings.UNFOLD_TIME)) time = cells;
        }
        require(time != null, "unfold time setting registered");
        require(time.get(2).equals("Double"), "unfold time is a double");
        require(Float.parseFloat(time.get(3)) == LayoutSettings.UNFOLD_TIME_DEFAULT, "unfold time default");
        require(Float.parseFloat(time.get(6)) == LayoutSettings.UNFOLD_TIME_MIN, "unfold time minimum");
        require(Float.parseFloat(time.get(7)) == LayoutSettings.UNFOLD_TIME_MAX, "unfold time maximum");
        require(LayoutSettings.UNFOLD_TIME_DEFAULT == 0.1f, "unfold time defaults to 0.1 s");
        require(!time.get(5).isBlank(), "unfold time help text");
        require(LayoutSettings.unfoldTime((Double) null) == LayoutSettings.UNFOLD_TIME_DEFAULT, "missing time falls back");
        require(LayoutSettings.unfoldTime(Double.NaN) == LayoutSettings.UNFOLD_TIME_DEFAULT, "nan time falls back");
        require(LayoutSettings.unfoldTime(-1.0) == LayoutSettings.UNFOLD_TIME_MIN, "time clamped at the minimum");
        require(LayoutSettings.unfoldTime(5.0) == LayoutSettings.UNFOLD_TIME_MAX, "time clamped at the maximum");
        require(LayoutSettings.unfoldTime(0.3) == 0.3f, "time kept");
        require(row != null, "layout setting registered");
        require(row.get(2).equals("Radio"), "layout is a radio");
        require(ButtonLayout.DEFAULT == ButtonLayout.FIVE_PER_ROW, "five per row is the default");
        require(row.get(3).equals(ButtonLayout.DEFAULT.label), "csv default matches ButtonLayout.DEFAULT");
        require(row.get(5).contains(ButtonLayout.DEFAULT.label + " (default)"), "help text names the default");
        List<String> options = new ArrayList<>();
        for (String option : row.get(4).split(",")) options.add(option.trim());
        List<String> labels = new ArrayList<>();
        for (ButtonLayout layout : ButtonLayout.values()) labels.add(layout.label);
        require(options.equals(labels), "radio options match ButtonLayout: " + options);
        require(!row.get(5).isBlank(), "layout help text");
        for (ButtonLayout layout : ButtonLayout.values()) {
            require(ButtonLayout.byLabel(layout.label) == layout, "label lookup " + layout);
        }
        require(ButtonLayout.byLabel(null) == ButtonLayout.DEFAULT, "missing value falls back");
        require(ButtonLayout.byLabel("x") == ButtonLayout.DEFAULT, "unknown value falls back");
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
