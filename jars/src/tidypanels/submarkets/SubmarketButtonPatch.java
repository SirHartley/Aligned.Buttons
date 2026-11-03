package tidypanels.submarkets;

import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.MethodType;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import java.util.ArrayList;
import java.util.List;

// 0.98a-RC8: com.fs.starfarer.campaign.ui.Z, the class name will change, its package and showSubmarketTextDialog should not
@Patch(target = @ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui",
        methodMatches = @MethodMatch(methodName = "showSubmarketTextDialog")))
public class SubmarketButtonPatch {

    // the strip's only list field holds its buttons in display order
    private static final FieldQuerySpec BUTTONS = FieldQuery.create().subtype(List.class).build();
    // declared on the button class, returns its render panel
    private static final MethodQuerySpec BUTTON_PANEL = MethodQuery.create().methodName("getButtonPanel")
            .parameterCount(0).build();
    // the render panel's only label field is the submarket name
    private static final FieldQuerySpec LABEL = FieldQuery.create().subtype(LabelAPI.class).build();

    // the constructor builds the buttons; the cargo and fleet screens read the strip height after it returns
    @After(target = @MethodMatch(methodType = MethodType.CONSTRUCTOR, parameterCount = 6))
    public static void afterConstructor(AfterContext context) {
        if (!(context.getSelf() instanceof UIPanelAPI strip) || !context.hasField(BUTTONS)) return;
        List<ButtonAPI> buttons = new ArrayList<>();
        for (Object item : (List<?>) context.getField(BUTTONS).get()) {
            if (item instanceof ButtonAPI button) buttons.add(button);
        }
        if (buttons.isEmpty()) return;
        ButtonLayout layout = LayoutSettings.layout();
        LayoutSettings.readRenderSettings();
        PositionAPI first = buttons.get(0).getPosition();
        SubmarketGrid grid = SubmarketGrid.of(layout, strip.getPosition().getWidth(), first.getHeight(),
                first.getWidth(), buttons.size(), storageIndex(buttons));
        if (layout == ButtonLayout.UNFOLD) {
            unfold(context, buttons, new UnfoldRows(grid, first.getWidth(), LayoutSettings.unfoldTime()));
        } else {
            place(context, buttons, grid, layout);
        }
        strip.getPosition().setSize(grid.width, grid.height());
    }

    private static void place(AfterContext context, List<ButtonAPI> buttons, SubmarketGrid grid, ButtonLayout layout) {
        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI button = buttons.get(i);
            int slot = grid.slot(i);
            button.getPosition().setSize(grid.buttonWidth(slot), grid.buttonHeight)
                    .inTL(grid.left(slot), grid.top(slot));
            if (layout != ButtonLayout.FIVE_PER_ROW) continue;
            LabelAPI label = label(context, button, grid.buttonWidth(slot), grid.buttonHeight);
            if (label == null) continue;
            label.setText(SubmarketGrid.fit(label.getText(), grid.labelWidth(slot), label::computeTextWidth));
        }
    }

    // runs every frame while the cargo or fleet screen is open, only unfold strips carry state
    @After(target = @MethodMatch(methodName = "advanceImpl", parameterCount = 1))
    public static void afterAdvance(AfterContext context) {
        Object state = context.getData().get(UnfoldStrip.KEY);
        if (!(state instanceof UnfoldStrip unfold) || !(context.getSelf() instanceof UIComponentAPI strip)) return;
        unfold.advance(strip, (Float) context.getArg(0));
    }

    private static void unfold(AfterContext context, List<ButtonAPI> buttons, UnfoldRows rows) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).isHighlighted()) rows.openNow(i);
        }
        float height = rows.grid.buttonHeight;
        for (int i = 0; i < buttons.size(); i++) {
            LabelAPI label = label(context, buttons.get(i), rows.width(i), height);
            if (label == null) continue;
            label.setText(SubmarketGrid.fit(label.getText(),
                    rows.openWidth - SubmarketGrid.labelInset(height), label::computeTextWidth));
        }
        UnfoldStrip strip = new UnfoldStrip(rows, buttons);
        strip.apply();
        context.getData().put(UnfoldStrip.KEY, strip);
    }

    private static LabelAPI label(AfterContext context, ButtonAPI button, float width, float height) {
        if (!context.hasMethod(BUTTON_PANEL, button)
                || !(context.getMethod(BUTTON_PANEL, button).call() instanceof UIComponentAPI panel)) return null;
        // the render panel builds its name label on its first size change, otherwise on the first frame
        panel.getPosition().setSize(width, height);
        if (!context.hasField(LABEL, panel) || !(context.getField(LABEL, panel).get() instanceof LabelAPI label)) {
            return null;
        }
        return label;
    }

    private static int storageIndex(List<ButtonAPI> buttons) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).getCustomData() instanceof SubmarketAPI submarket
                    && Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())) return i;
        }
        return -1;
    }
}
