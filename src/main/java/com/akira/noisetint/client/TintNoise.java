package com.akira.noisetint.client;

import java.awt.Color;

public final class TintNoise {
    public static int jitterHSB(int rgb, int x, int z) {
        int h = hash32(x, z);
        float[] hsb = Color.RGBtoHSB((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, null);

        float dh = ((h       & 255) / 255f - 0.5f) * 0.015f; // ±1.5% hue
        float ds = (((h>>8)  & 255) / 255f - 0.5f) * 0.05f;  // ±5% sat
        float db = (((h>>16) & 255) / 255f - 0.5f) * 0.05f;  // ±5% bright

        float nh = hsb[0] + dh; nh -= (float)Math.floor(nh);
        float ns = clamp(hsb[1] + ds, 0f, 1f);
        float nb = clamp(hsb[2] + db, 0f, 1f);
        return Color.HSBtoRGB(nh, ns, nb) & 0xFFFFFF;
    }

    private static int hash32(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
    private TintNoise() {}
}
