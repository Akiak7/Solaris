package com.akira.noisetint.util;

public class ColorUtil {

    public static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public static float wrapHue(float deg) {
        deg %= 360f;
        if (deg < 0f) deg += 360f;
        return deg;
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h, s;
        float v = max;

        float d = max - min;
        s = max == 0 ? 0 : d / max;

        if (d == 0) {
            h = 0;
        } else {
            if (max == r) {
                h = ((g - b) / d + (g < b ? 6 : 0));
            } else if (max == g) {
                h = ((b - r) / d + 2);
            } else {
                h = ((r - g) / d + 4);
            }
            h *= 60f;
        }
        return new float[]{h, s, v};
    }

    public static int hsvToRgbInt(float[] hsv) {
        float h = hsv[0];
        float s = clamp01(hsv[1]);
        float v = clamp01(hsv[2]);

        if (s == 0) {
            int vi = (int)(v * 255.0f + 0.5f);
            return (vi << 16) | (vi << 8) | vi;
        }

        h = (h % 360f + 360f) % 360f;
        float hf = h / 60f;
        int i = (int)Math.floor(hf);
        float f = hf - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));

        float r=0,g=0,b=0;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: default: r = v; g = p; b = q; break;
        }
        int ri = (int)(r * 255.0f + 0.5f);
        int gi = (int)(g * 255.0f + 0.5f);
        int bi = (int)(b * 255.0f + 0.5f);
        return (ri << 16) | (gi << 8) | bi;
    }

    public static int rgbFloatToInt(float r, float g, float b) {
        int ri = (int)(r * 255.0f + 0.5f);
        int gi = (int)(g * 255.0f + 0.5f);
        int bi = (int)(b * 255.0f + 0.5f);
        return (ri << 16) | (gi << 8) | bi;
    }
}