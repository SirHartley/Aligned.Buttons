package tidypanels.submarkets;

import java.util.Arrays;

public final class UnfoldRows {

    private static final float SNAP = 1e-4f;

    public final SubmarketGrid grid;
    public final float foldedWidth;
    public final float openWidth;
    // seconds for a full unfold, 0 snaps
    public final float unfoldTime;

    private final int[] rowOf;
    private final int[][] members;
    // per tile 0 folded to 1 open; each row sums to 1, so its total width never changes
    private final float[] open;
    private final int[] target;

    public UnfoldRows(SubmarketGrid grid, float openWidth, float unfoldTime) {
        this.grid = grid;
        this.foldedWidth = grid.foldedWidth;
        this.openWidth = Math.max(openWidth, foldedWidth);
        this.unfoldTime = Math.max(0f, unfoldTime);
        int[] order = new int[grid.count];
        int next = 0;
        for (int i = 0; i < grid.count; i++) {
            if (i != grid.storage) order[next++] = i;
        }
        if (grid.storage >= 0 && grid.storage < grid.count) order[next] = grid.storage;
        rowOf = new int[grid.count];
        members = new int[grid.rows()][];
        open = new float[grid.count];
        target = new int[members.length];
        for (int row = 0; row < members.length; row++) {
            int from = Math.min(grid.count, row * grid.columns);
            members[row] = Arrays.copyOfRange(order, from, Math.min(grid.count, from + grid.columns));
            for (int i : members[row]) rowOf[i] = row;
            target[row] = members[row].length > 0 ? members[row][0] : -1;
            if (target[row] >= 0) open[target[row]] = 1f;
        }
    }

    public int rows() {
        return members.length;
    }

    public int row(int index) {
        return rowOf[index];
    }

    public int target(int row) {
        return target[row];
    }

    public float open(int index) {
        return open[index];
    }

    public void openNow(int index) {
        int row = rowOf[index];
        for (int i : members[row]) open[i] = 0f;
        open[index] = 1f;
        target[row] = index;
    }

    public void aim(int index) {
        target[rowOf[index]] = index;
    }

    // the target takes width from the others in proportion to what they hold, so several tiles move at once
    public boolean step(float amount) {
        if (amount <= 0f) return false;
        boolean changed = false;
        for (int row = 0; row < members.length; row++) {
            int aim = target[row];
            if (aim < 0 || open[aim] >= 1f) continue;
            float others = 0f;
            for (int i : members[row]) {
                if (i != aim) others += open[i];
            }
            float grow = unfoldTime <= 0f ? 1f - open[aim] : Math.min(amount / unfoldTime, 1f - open[aim]);
            if (others > 0f) {
                for (int i : members[row]) {
                    if (i != aim) open[i] = Math.max(0f, open[i] - grow * open[i] / others);
                }
            }
            open[aim] += grow;
            if (others <= SNAP || 1f - open[aim] < SNAP) openNow(aim);
            changed = true;
        }
        return changed;
    }

    public float width(int index) {
        return foldedWidth + open[index] * (openWidth - foldedWidth);
    }

    // storage stays on the strip's right edge, the rest packs from the left
    public float left(int index) {
        if (index == grid.storage) return grid.width - width(index);
        float x = 0f;
        for (int i : members[rowOf[index]]) {
            if (i == index) return x;
            if (i != grid.storage) x += width(i) + SubmarketGrid.GAP;
        }
        return x;
    }

    public float top(int index) {
        return rowOf[index] * (grid.buttonHeight + SubmarketGrid.ROW_GAP);
    }

    // x from the strip's left edge, y down from its top edge; -1 over a gap or outside
    public int at(float x, float y) {
        int row = (int) Math.floor(y / (grid.buttonHeight + SubmarketGrid.ROW_GAP));
        if (row < 0 || row >= members.length || y - row * (grid.buttonHeight + SubmarketGrid.ROW_GAP) >= grid.buttonHeight) {
            return -1;
        }
        for (int i : members[row]) {
            float left = left(i);
            if (x >= left && x < left + width(i)) return i;
        }
        return -1;
    }
}
