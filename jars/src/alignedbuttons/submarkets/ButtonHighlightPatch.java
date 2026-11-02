package alignedbuttons.submarkets;

import com.fs.starfarer.api.ui.UIPanelAPI;
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
public class ButtonHighlightPatch {

    // the highlight strip behind the name is width - 73 px wide, gl still draws a negative quad as a 1 px line
    @RedirectMethodCall(target = @MethodMatch(methodName = "renderImpl", parameterCount = 1),
            call = @MethodMatch(parameterTypes = {float.class, float.class, float.class, float.class, Color.class,
                    float.class}, returnType = void.class))
    public static void skipNegativeFill(MethodCallContext context) {
        if ((Float) context.getCallArg(2) >= 0f) context.call();
    }
}
