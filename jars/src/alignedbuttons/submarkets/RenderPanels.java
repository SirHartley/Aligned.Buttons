package alignedbuttons.submarkets;

import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import patchlib.api.context.Context;

final class RenderPanels {

    // the button sets itself on its render panel at the start of every render
    private static final String BUTTON = "button";

    private RenderPanels() {
    }

    // the cargo screen's colony info button shares the render panel class, so only submarket buttons count
    static ButtonAPI submarketButton(Context context) {
        if (!context.hasField(BUTTON)) return null;
        Object button = context.getField(BUTTON).get();
        return button instanceof ButtonAPI result && result.getCustomData() instanceof SubmarketAPI ? result : null;
    }
}
