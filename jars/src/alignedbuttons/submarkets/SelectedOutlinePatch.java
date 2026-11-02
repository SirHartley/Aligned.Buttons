package alignedbuttons.submarkets;

import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.lwjgl.opengl.GL11;
import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.MethodType;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

import java.awt.Color;

// 0.98a-RC8: com.fs.starfarer.campaign.ui.ooOO, the submarket button render panel; the only class in its package with this constructor
@Patch(target = @ClassMatch(subtype = UIPanelAPI.class, packageName = "com.fs.starfarer.campaign.ui",
        methodMatches = @MethodMatch(methodType = MethodType.CONSTRUCTOR,
                parameterTypes = {String.class, String.class, Color.class, Color.class, Color.class})))
public class SelectedOutlinePatch {

    // below this the faction color has no real hue to saturate; pale tints above it still have one
    public static final float GRAY = 0.03f;
    // the animated inner ring is this far from the faction color to white, so white highlights show on it
    private static final float TINT = 0.5f;
    // like the vanilla selected weapon slot: four highlights a quarter lap apart, clockwise, one lap in 4.4 s
    public static final int HIGHLIGHTS = 4;
    public static final float LAP_SECONDS = 4.4f;
    // each highlight fades from its head over this share of a lap
    public static final float HIGHLIGHT_LENGTH = 0.08f;

    // after the frame, icon and name, so the outline sits on top
    @After(target = @MethodMatch(methodName = "renderImpl", parameterCount = 1))
    public static void afterRender(AfterContext context) {
        ButtonAPI button = RenderPanels.submarketButton(context);
        if (button == null || !button.isHighlighted() || !(context.getSelf() instanceof UIComponentAPI panel)) return;
        // base, not bright: an unset bright color is the base tinted toward the ui cyan, which turns grays blue
        Color color = saturated(((SubmarketAPI) button.getCustomData()).getFaction().getBaseUIColor());
        boolean animated = LayoutSettings.animatedOutline();
        Color inside = animated ? mix(color, Color.WHITE, TINT) : Color.WHITE;
        draw(panel.getPosition(), new Color[]{Color.BLACK, color, inside}, animated, (Float) context.getArg(0));
    }

    // a gray keeps its own brightness, at full brightness it would be white and merge with the inner ring
    public static Color saturated(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return hsb[1] < GRAY ? new Color(color.getRed(), color.getGreen(), color.getBlue()) : Color.getHSBColor(hsb[0], 1f, 1f);
    }

    public static Color mix(Color from, Color to, float amount) {
        return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
    }

    // head of highlight 0 as a fraction of the lap; rings run counterclockwise, so clockwise counts down
    public static float lapPosition(double seconds) {
        double lap = seconds / LAP_SECONDS;
        return (float) (1.0 - (lap - Math.floor(lap)));
    }

    // rings from the tile edge inwards, the highlights run on the innermost one
    private static void draw(PositionAPI position, Color[] rings, boolean animated, float alpha) {
        float x0 = position.getX();
        float y0 = position.getY();
        float x1 = x0 + position.getWidth();
        float y1 = y0 + position.getHeight();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_QUADS);
        OutlineRing innermost = null;
        for (int ring = 0; ring < rings.length; ring++) {
            OutlineRing path = OutlineRing.of(x0, y0, x1, y1, ring);
            Color color = rings[ring];
            path.pieces(0f, 1f, (ox0, oy0, ix0, iy0, ox1, oy1, ix1, iy1, at0, at1) ->
                    quad(color, alpha, alpha, ox0, oy0, ix0, iy0, ox1, oy1, ix1, iy1));
            innermost = path;
        }
        if (animated && innermost != null) {
            float head = lapPosition(System.nanoTime() / 1e9);
            for (int i = 0; i < HIGHLIGHTS; i++) {
                float from = head + (float) i / HIGHLIGHTS;
                // the tail trails behind the clockwise head, fading out
                innermost.pieces(from, from + HIGHLIGHT_LENGTH, (ox0, oy0, ix0, iy0, ox1, oy1, ix1, iy1, at0, at1) ->
                        quad(Color.WHITE, alpha * (1f - at0), alpha * (1f - at1), ox0, oy0, ix0, iy0, ox1, oy1, ix1, iy1));
            }
        }
        GL11.glEnd();
        GL11.glPopAttrib();
    }

    private static void quad(Color color, float alpha0, float alpha1, float ox0, float oy0, float ix0, float iy0,
                             float ox1, float oy1, float ix1, float iy1) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        GL11.glColor4f(r, g, b, alpha0);
        GL11.glVertex2f(ox0, oy0);
        GL11.glVertex2f(ix0, iy0);
        GL11.glColor4f(r, g, b, alpha1);
        GL11.glVertex2f(ix1, iy1);
        GL11.glVertex2f(ox1, oy1);
    }
}
