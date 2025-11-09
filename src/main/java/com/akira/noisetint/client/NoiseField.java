package com.akira.noisetint.client;

import java.util.Random;

/**
 * World-seeded 2D noise field with light domain warp producing HSV deltas.
 */
public class NoiseField {

    private static long worldSeed = 0L;
    private static OpenSimplexNoise base;
    private static OpenSimplexNoise warpX;
    private static OpenSimplexNoise warpZ;

private static final double MAIN_SCALE = 1.0 / 256.0;   // larger regions
private static final double WARP_SCALE = 1.0 / 128.0;
private static final double WARP_AMPLITUDE = 48.0;

private static final float HUE_DEG_AMPL = 60f;   // was 14
private static final float SAT_MUL_AMPL = 0.30f; // was 0.06
private static final float VAL_MUL_AMPL = 0.30f; // was 0.08


    public static void reseed(long seed) {
        worldSeed = seed;
        Random r = new Random(seed ^ 0x9E3779B97F4A7C15L);
        base  = new OpenSimplexNoise(r.nextLong());
        warpX = new OpenSimplexNoise(r.nextLong());
        warpZ = new OpenSimplexNoise(r.nextLong());
    }

    public static float[] sampleTint(int wx, int wz) {
        if (base == null) reseed(worldSeed);
        // domain warp
        double dx = warpX.eval(wx * WARP_SCALE, wz * WARP_SCALE) * WARP_AMPLITUDE;
        double dz = warpZ.eval(wx * WARP_SCALE, wz * WARP_SCALE) * WARP_AMPLITUDE;

        double nx0 = base.eval((wx + 113.0 + dx) * MAIN_SCALE, (wz - 37.0 + dz) * MAIN_SCALE);
        double nx1 = base.eval((wx - 59.0 + dx) * MAIN_SCALE, (wz + 71.0 + dz) * MAIN_SCALE);
        double nx2 = base.eval((wx + 211.0 + dx) * MAIN_SCALE, (wz + 19.0 + dz) * MAIN_SCALE);

        // map [-1,1] to our ranges
        float hueDeg = (float) (nx0 * HUE_DEG_AMPL);
        float satMul = 1.0f + (float) (nx1 * SAT_MUL_AMPL);
        float valMul = 1.0f + (float) (nx2 * VAL_MUL_AMPL);

        // clamp sanity
        if (satMul < 0.5f) satMul = 0.5f;
        if (valMul < 0.5f) valMul = 0.5f;
        if (satMul > 1.5f) satMul = 1.5f;
        if (valMul > 1.5f) valMul = 1.5f;

        return new float[]{hueDeg, satMul, valMul};
    }
}