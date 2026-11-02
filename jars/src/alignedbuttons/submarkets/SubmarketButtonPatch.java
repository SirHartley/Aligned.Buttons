package alignedbuttons.submarkets;

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
        ButtonLayout layout = ButtonLayout.current();
        PositionAPI first = buttons.get(0).getPosition();
        SubmarketGrid grid = SubmarketGrid.of(layout, strip.getPosition().getWidth(), first.getHeight(),
                first.getWidth(), buttons.size(), storageIndex(buttons));
        for (int i = 0; i < buttons.size(); i++) {
            ButtonAPI button = buttons.get(i);
            int slot = grid.slot(i);
            button.getPosition().setSize(grid.buttonWidth(slot), grid.buttonHeight)
                    .inTL(grid.left(slot), grid.top(slot));
            if (layout != ButtonLayout.STRETCHED) relabel(context, button, grid, slot, layout);
        }
        strip.getPosition().setSize(grid.width, grid.height());
    }

    private static void relabel(AfterContext context, ButtonAPI button, SubmarketGrid grid, int slot,
                                ButtonLayout layout) {
        if (!context.hasMethod(BUTTON_PANEL, button)
                || !(context.getMethod(BUTTON_PANEL, button).call() instanceof UIComponentAPI panel)) return;
        // the render panel builds its name label on its first size change, otherwise on the first frame
        panel.getPosition().setSize(grid.buttonWidth(slot), grid.buttonHeight);
        if (!context.hasField(LABEL, panel) || !(context.getField(LABEL, panel).get() instanceof LabelAPI label)) return;
        label.setText(layout == ButtonLayout.ICONS ? ""
                : SubmarketGrid.fit(label.getText(), grid.labelWidth(slot), label::computeTextWidth));
    }

    private static int storageIndex(List<ButtonAPI> buttons) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).getCustomData() instanceof SubmarketAPI submarket
                    && Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())) return i;
        }
        return -1;
    }
}
