package alignedbuttons.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.MethodType;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;

import java.awt.Color;
import java.nio.IntBuffer;

// 0.98a-RC8: com.fs.starfarer.campaign.ui.ooOO, the submarket button render panel; the only class in its package with this constructor
@Patch(target = @ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui",
        methodMatches = @MethodMatch(methodType = MethodType.CONSTRUCTOR,
                parameterTypes = {String.class, String.class, Color.class, Color.class, Color.class})))
public class LabelClipPatch {

    // render thread only; lwjgl needs room for 16 ints
    private static final IntBuffer BOX = BufferUtils.createIntBuffer(16);

    // the name is the only LabelAPI rendered in renderImpl, the icon renders through a class that is no LabelAPI
    @RedirectMethodCall(target = @MethodMatch(methodName = "renderImpl", parameterCount = 1),
            call = @MethodMatch(methodName = "render", parameterTypes = float.class),
            owner = @ClassMatch(subtype = LabelAPI.class))
    public static void clipName(MethodCallContext context) {
        if (RenderPanels.submarketButton(context) == null || !(context.getSelf() instanceof UIComponentAPI panel)) {
            context.call();
            return;
        }
        PositionAPI position = panel.getPosition();
        SettingsAPI settings = Global.getSettings();
        // same virtual to framebuffer scale the vanilla clip stack uses
        float scaleX = settings.getScreenWidthPixels() / settings.getScreenWidth();
        float scaleY = settings.getScreenHeightPixels() / settings.getScreenHeight();
        int left = Math.round((position.getX() + SubmarketGrid.BORDER) * scaleX);
        int right = Math.round((position.getX() + position.getWidth() - SubmarketGrid.BORDER) * scaleX);
        int bottom = Math.round(position.getY() * scaleY);
        int top = Math.round((position.getY() + position.getHeight()) * scaleY);
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, BOX);
            left = Math.max(left, BOX.get(0));
            bottom = Math.max(bottom, BOX.get(1));
            right = Math.min(right, BOX.get(0) + BOX.get(2));
            top = Math.min(top, BOX.get(1) + BOX.get(3));
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        try {
            context.call();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
