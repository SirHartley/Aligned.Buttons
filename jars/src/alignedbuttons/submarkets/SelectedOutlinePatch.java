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

    // the vanilla border cuts its corners by 3 px
    private static final float CUT = 3f;
    // each ring is 1 px; its corner cut shrinks so the diagonal stays as thick as the sides
    private static final float CUT_STEP = (float) (Math.sqrt(2) - 1);
    // below this the faction color has no real hue, so saturating it would turn it red
    private static final float GRAY = 0.1f;

    // after the frame, icon and name, so the outline sits on top
    @After(target = @MethodMatch(methodName = "renderImpl", parameterCount = 1))
    public static void afterRender(AfterContext context) {
        ButtonAPI button = RenderPanels.submarketButton(context);
        if (button == null || !button.isHighlighted() || !(context.getSelf() instanceof UIComponentAPI panel)) return;
        Color color = saturated(((SubmarketAPI) button.getCustomData()).getFaction().getBrightUIColor());
        draw(panel.getPosition(), new Color[]{Color.BLACK, color, Color.WHITE}, (Float) context.getArg(0));
    }

    public static Color saturated(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] < GRAY ? hsb[1] : 1f, 1f);
    }

    // rings from the tile edge inwards
    private static void draw(PositionAPI position, Color[] rings, float alpha) {
        float x0 = position.getX();
        float y0 = position.getY();
        float x1 = x0 + position.getWidth();
        float y1 = y0 + position.getHeight();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int ring = 0; ring < rings.length; ring++) {
            float[] outer = octagon(x0 + ring, y0 + ring, x1 - ring, y1 - ring, Math.max(0f, CUT - ring * CUT_STEP));
            float[] inner = octagon(x0 + ring + 1, y0 + ring + 1, x1 - ring - 1, y1 - ring - 1,
                    Math.max(0f, CUT - (ring + 1) * CUT_STEP));
            Color color = rings[ring];
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha);
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= 8; i++) {
                int k = (i % 8) * 2;
                GL11.glVertex2f(outer[k], outer[k + 1]);
                GL11.glVertex2f(inner[k], inner[k + 1]);
            }
            GL11.glEnd();
        }
        GL11.glPopAttrib();
    }

    private static float[] octagon(float x0, float y0, float x1, float y1, float cut) {
        return new float[]{x0 + cut, y0, x1 - cut, y0, x1, y0 + cut, x1, y1 - cut,
                x1 - cut, y1, x0 + cut, y1, x0, y1 - cut, x0, y0 + cut};
    }
}
