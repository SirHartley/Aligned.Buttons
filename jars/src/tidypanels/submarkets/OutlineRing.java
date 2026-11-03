package tidypanels.submarkets;

public final class OutlineRing {

    // the vanilla border cuts its corners by 3 px
    public static final float CUT = 3f;
    // each ring is 1 px; its corner cut shrinks so the diagonal stays as thick as the sides
    private static final float CUT_STEP = (float) (Math.sqrt(2) - 1);

    // 8 vertices each as x, y pairs, counterclockwise on screen (gl y up) from the bottom edge
    public final float[] outer;
    public final float[] inner;
    public final float length;
    // outer path length at each vertex, the 9th entry closes the loop
    private final float[] start = new float[9];

    public interface Piece {
        // outer and inner corners at both ends, plus where each end sits in the requested range (0 to 1)
        void quad(float ox0, float oy0, float ix0, float iy0, float ox1, float oy1, float ix1, float iy1,
                  float at0, float at1);
    }

    private OutlineRing(float[] outer, float[] inner) {
        this.outer = outer;
        this.inner = inner;
        for (int e = 0; e < 8; e++) {
            int i = e * 2;
            int j = ((e + 1) % 8) * 2;
            start[e + 1] = start[e] + (float) Math.hypot(outer[j] - outer[i], outer[j + 1] - outer[i + 1]);
        }
        length = start[8];
    }

    // ring 0 is the tile's outermost pixel, each next ring sits 1 px further in
    public static OutlineRing of(float x0, float y0, float x1, float y1, int ring) {
        return new OutlineRing(
                octagon(x0 + ring, y0 + ring, x1 - ring, y1 - ring, Math.max(0f, CUT - ring * CUT_STEP)),
                octagon(x0 + ring + 1, y0 + ring + 1, x1 - ring - 1, y1 - ring - 1,
                        Math.max(0f, CUT - (ring + 1) * CUT_STEP)));
    }

    private static float[] octagon(float x0, float y0, float x1, float y1, float cut) {
        return new float[]{x0 + cut, y0, x1 - cut, y0, x1, y0 + cut, x1, y1 - cut,
                x1 - cut, y1, x0 + cut, y1, x0, y1 - cut, x0, y0 + cut};
    }

    // from and to are fractions of a lap, any value; the range wraps past 1
    public void pieces(float from, float to, Piece out) {
        float span = to - from;
        if (span <= 0f) return;
        span = Math.min(span, 1f);
        float a = from - (float) Math.floor(from);
        if (a + span <= 1f) {
            emit(a, a + span, 0f, 1f, out);
        } else {
            float split = (1f - a) / span;
            emit(a, 1f, 0f, split, out);
            emit(0f, a + span - 1f, split, 1f, out);
        }
    }

    private void emit(float a, float b, float atA, float atB, Piece out) {
        float sa = a * length;
        float sb = b * length;
        for (int e = 0; e < 8; e++) {
            float e0 = start[e];
            float e1 = start[e + 1];
            float lo = Math.max(sa, e0);
            float hi = Math.min(sb, e1);
            if (hi <= lo || e1 <= e0) continue;
            float t0 = (lo - e0) / (e1 - e0);
            float t1 = (hi - e0) / (e1 - e0);
            float at0 = atA + (atB - atA) * (lo - sa) / (sb - sa);
            float at1 = atA + (atB - atA) * (hi - sa) / (sb - sa);
            int i = e * 2;
            int j = ((e + 1) % 8) * 2;
            out.quad(lerp(outer[i], outer[j], t0), lerp(outer[i + 1], outer[j + 1], t0),
                    lerp(inner[i], inner[j], t0), lerp(inner[i + 1], inner[j + 1], t0),
                    lerp(outer[i], outer[j], t1), lerp(outer[i + 1], outer[j + 1], t1),
                    lerp(inner[i], inner[j], t1), lerp(inner[i + 1], inner[j + 1], t1), at0, at1);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
