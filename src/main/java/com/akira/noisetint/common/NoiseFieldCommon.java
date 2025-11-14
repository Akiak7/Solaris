package com.akira.noisetint.common;

import java.util.Random;

/** World-seeded 2D OpenSimplex with gentle domain warp. Server-safe. */
public final class NoiseFieldCommon {
    private static long worldSeed = 0L;
    private static OpenSimplexNoise base, warpX, warpZ;
    private static OpenSimplexNoise baseTerrain, warpXTerrain, warpZTerrain;
    private static final long TERRAIN_SALT = 0xC0FFEEBEEF12345AL;

    private static final double MAIN_SCALE = 1.0 / 4096.0;  // large regions
    private static final double WARP_SCALE = 1.0 / 8192.0;
    private static final double WARP_AMPL  = 8.0;

    // visual-tuned ranges; we’ll map to height deltas in Variant
    private static final float HUE_DEG_AMPL = 60f;   // UNUSED
    private static final float SAT_MUL_AMPL = 0.06f; // HEIGHT VARIATION
    private static final float VAL_MUL_AMPL = 0.08f; // DEPTH/HEIGHT

    public static void reseed(long seed) {
        worldSeed = seed;
        Random r = new Random(seed ^ 0x9E3779B97F4A7C15L);
        base  = new OpenSimplexNoise(r.nextLong());
        warpX = new OpenSimplexNoise(r.nextLong());
        warpZ = new OpenSimplexNoise(r.nextLong());
        Random rt = new Random((worldSeed ^ TERRAIN_SALT) + 0x9E3779B97F4A7C15L);
        baseTerrain  = new OpenSimplexNoise(rt.nextLong());
        warpXTerrain = new OpenSimplexNoise(rt.nextLong());
        warpZTerrain = new OpenSimplexNoise(rt.nextLong());
    }

    /** Returns {hueDeg,satMul,valMul}-like triple, reused to derive height deltas. */
    public static float[] sampleTint(int x, int z) {
        if (base == null) reseed(worldSeed);
        double dx = warpX.eval(x * WARP_SCALE, z * WARP_SCALE) * WARP_AMPL;
        double dz = warpZ.eval(x * WARP_SCALE, z * WARP_SCALE) * WARP_AMPL;

        double n0 = base.eval((x + 113.0 + dx) * MAIN_SCALE, (z - 37.0 + dz) * MAIN_SCALE);
        double n1 = base.eval((x - 59.0 + dx) * MAIN_SCALE, (z + 71.0 + dz) * MAIN_SCALE);
        double n2 = base.eval((x + 211.0 + dx) * MAIN_SCALE, (z + 19.0 + dz) * MAIN_SCALE);

        //n0 = soften(n0); n1 = soften(n1); n2 = soften(n2);

        float hueDeg = (float)(n0 * HUE_DEG_AMPL);
        float satMul = 1.0f + (float)(n1 * SAT_MUL_AMPL);
        float valMul = 1.0f + (float)(n2 * VAL_MUL_AMPL);
        return new float[]{hueDeg, satMul, valMul};
    }

    // separate sampler (larger/softer defaults for terrain if you want)
    public static float[] sampleTintTerrain(int x, int z) {
        if (baseTerrain == null) reseed(worldSeed);
        double dx = warpXTerrain.eval(x * WARP_SCALE, z * WARP_SCALE) * WARP_AMPL;
        double dz = warpZTerrain.eval(x * WARP_SCALE, z * WARP_SCALE) * WARP_AMPL;

        double n0 = baseTerrain.eval((x + 113.0 + dx) * MAIN_SCALE, (z - 37.0 + dz) * MAIN_SCALE);
        double n1 = baseTerrain.eval((x - 59.0 + dx) * MAIN_SCALE, (z + 71.0 + dz) * MAIN_SCALE);
        double n2 = baseTerrain.eval((x + 211.0 + dx) * MAIN_SCALE, (z + 19.0 + dz) * MAIN_SCALE);

        //n0 = soften(n0); n1 = soften(n1); n2 = soften(n2);

        float hueDeg = (float)(n0 * HUE_DEG_AMPL);
        float satMul = 1.0f + (float)(n1 * SAT_MUL_AMPL);
        float valMul = 1.0f + (float)(n2 * VAL_MUL_AMPL);
        return new float[]{hueDeg, satMul, valMul};
    }

    private static double soften(double n) {
        double t = 0.5 * (n + 1.0);
        t = t * t * (3.0 - 2.0 * t);
        return t * 2.0 - 1.0;
    }
}
