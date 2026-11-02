package alignedbuttons.submarkets;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.lwjgl.opengl.GL11;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.MethodType;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;

import java.awt.Color;

// 0.98a-RC8: com.fs.starfarer.campaign.ui.ooOO, the submarket button render panel; the only class in its package with this constructor
@Patch(target = @ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui",
        methodMatches = @MethodMatch(methodType = MethodType.CONSTRUCTOR,
                parameterTypes = {String.class, String.class, Color.class, Color.class, Color.class})))
public class ChamferedFillPatch {

    // the dark background, (position, Color, alpha), is the only 3 parameter void call in renderImpl
    @RedirectMethodCall(target = @MethodMatch(methodName = "renderImpl", parameterCount = 1),
            call = @MethodMatch(parameterCount = 3, returnType = void.class))
    public static void background(MethodCallContext context) {
        if (RenderPanels.submarketButton(context) == null || !(context.getSelf() instanceof UIComponentAPI panel)
                || !(context.getCallArg(1) instanceof Color color) || !(context.getCallArg(2) instanceof Float alpha)) {
            context.call();
            return;
        }
        // same state the vanilla fill sets up
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        fill(panel.getPosition(), color, alpha);
    }

    // the disabled overlay covers the whole panel; it keeps the blend renderImpl set for it, the highlight strip passes on
    @RedirectMethodCall(target = @MethodMatch(methodName = "renderImpl", parameterCount = 1),
            call = @MethodMatch(parameterTypes = {float.class, float.class, float.class, float.class, Color.class,
                    float.class}, returnType = void.class))
    public static void overlay(MethodCallContext context) {
        if (RenderPanels.submarketButton(context) == null || !(context.getSelf() instanceof UIComponentAPI panel)
                || !covers(panel.getPosition(), context)) {
            context.call();
            return;
        }
        fill(panel.getPosition(), (Color) context.getCallArg(4), (Float) context.getCallArg(5));
    }

    private static boolean covers(PositionAPI position, MethodCallContext context) {
        return near(context.getCallArg(0), position.getX()) && near(context.getCallArg(1), position.getY())
                && near(context.getCallArg(2), position.getWidth()) && near(context.getCallArg(3), position.getHeight());
    }

    private static boolean near(Object value, float expected) {
        return value instanceof Float f && Math.abs(f - expected) < 0.5f;
    }

    // ring 0's outer octagon is the tile edge with the vanilla frame's 3 px corner cut
    private static void fill(PositionAPI position, Color color, float alpha) {
        float x0 = position.getX();
        float y0 = position.getY();
        float[] edge = OutlineRing.of(x0, y0, x0 + position.getWidth(), y0 + position.getHeight(), 0).outer;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
                color.getAlpha() / 255f * alpha);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < edge.length; i += 2) GL11.glVertex2f(edge[i], edge[i + 1]);
        GL11.glEnd();
    }
}
