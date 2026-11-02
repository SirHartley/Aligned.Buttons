package alignedbuttons.submarkets;

import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.MethodType;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import patchlib.api.query.FieldQuery;
import patchlib.api.spec.FieldQuerySpec;

import java.util.ArrayList;
import java.util.List;

// 0.98a-RC8: com.fs.starfarer.campaign.ui.Z, the class name will change, its package and showSubmarketTextDialog should not
@Patch(target = @ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui",
        methodMatches = @MethodMatch(methodName = "showSubmarketTextDialog")))
public class SubmarketButtonPatch {

    // the strip's only list field holds its buttons in display order
    private static final FieldQuerySpec BUTTONS = FieldQuery.create().subtype(List.class).build();

    // the constructor builds the buttons; the cargo and fleet screens read the strip height after it returns
    @After(target = @MethodMatch(methodType = MethodType.CONSTRUCTOR, parameterCount = 6))
    public static void afterConstructor(AfterContext context) {
        if (!(context.getSelf() instanceof UIPanelAPI strip) || !context.hasField(BUTTONS)) return;
        List<ButtonAPI> buttons = new ArrayList<>();
        for (Object item : (List<?>) context.getField(BUTTONS).get()) {
            if (item instanceof ButtonAPI button) buttons.add(button);
        }
        if (buttons.isEmpty()) return;
        PositionAPI first = buttons.get(0).getPosition();
        SubmarketGrid grid = SubmarketGrid.of(strip.getPosition().getWidth(), first.getHeight(), first.getWidth(),
                buttons.size(), storageIndex(buttons));
        for (int i = 0; i < buttons.size(); i++) {
            int slot = grid.slot(i);
            buttons.get(i).getPosition().setSize(grid.buttonWidth(slot), grid.buttonHeight)
                    .inTL(grid.left(slot), grid.top(slot));
        }
        strip.getPosition().setSize(grid.width, grid.height());
    }

    private static int storageIndex(List<ButtonAPI> buttons) {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).getCustomData() instanceof SubmarketAPI submarket
                    && Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())) return i;
        }
        return -1;
    }
}
