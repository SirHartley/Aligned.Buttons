package alignedbuttons.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class UnfoldStrip {

    // patch data key on the strip, the store is shared with other mods' patches
    static final String KEY = "aligned_buttons_unfold";

    private final UnfoldRows rows;
    // weak, this state lives in the strip's patch data and must not keep the strip's children alive
    private final List<WeakReference<ButtonAPI>> buttons = new ArrayList<>();
    private final List<WeakReference<LabelAPI>> labels = new ArrayList<>();
    private final float[] needed;
    private boolean settled;

    UnfoldStrip(UnfoldRows rows, List<ButtonAPI> buttons, LabelAPI[] labels, float[] needed) {
        this.rows = rows;
        for (ButtonAPI button : buttons) this.buttons.add(new WeakReference<>(button));
        for (LabelAPI label : labels) this.labels.add(label == null ? null : new WeakReference<>(label));
        this.needed = needed;
    }

    void apply() {
        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI button = buttons.get(i).get();
            if (button != null) {
                button.getPosition().setSize(rows.width(i), rows.grid.buttonHeight).inTL(rows.left(i), rows.top(i));
            }
            LabelAPI label = labels.get(i) == null ? null : labels.get(i).get();
            if (label != null) label.setOpacity(rows.labelOpacity(i, needed[i]));
        }
    }

    void advance(UIComponentAPI strip, float amount) {
        // the fleet screen selects its submarket after building the strip, so snap to it once
        if (!settled) {
            settled = true;
            openSelected();
            apply();
            return;
        }
        // the last hovered tile stays open after the cursor leaves
        PositionAPI position = strip.getPosition();
        float x = Global.getSettings().getMouseX() - position.getX();
        float y = position.getY() + position.getHeight() - Global.getSettings().getMouseY();
        if (x >= 0f && x < position.getWidth() && y >= 0f && y < position.getHeight()) {
            int hovered = rows.at(x, y);
            if (hovered >= 0) rows.aim(hovered);
        }
        if (rows.step(amount)) apply();
    }

    private void openSelected() {
        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI button = buttons.get(i).get();
            if (button != null && button.isHighlighted()) rows.openNow(i);
        }
    }
}
