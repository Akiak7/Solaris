package com.akira.noisetint.common;

public final class Variant {
    // Height delta amplitudes: keep subtle
    private static final float DEPTH_AMPL = 0.95f; // adds to base height (depth)
    private static final float SCALE_AMPL = 0.95f; // adds to roughness (heightVariation)

    /** Deterministic tiny bias to base height in [-DEPTH_AMPL, +DEPTH_AMPL]. */
    public static float depthDelta(int x, int z) {
        float[] t = NoiseFieldCommon.sampleTintTerrain(x, z);
        // Map value -> base height bias
        float v = t[2] - 1.0f; // ~[-0.08, +0.08] by default
        return clamp(v * (DEPTH_AMPL / 0.08f), -DEPTH_AMPL, DEPTH_AMPL);
    }

    /** Deterministic tiny bias to height variation in [-SCALE_AMPL, +SCALE_AMPL]. */
    public static float scaleDelta(int x, int z) {
        float[] t = NoiseFieldCommon.sampleTintTerrain(x, z);
        float s = t[1] - 1.0f;
        return clamp(s * (SCALE_AMPL / 0.06f), -SCALE_AMPL, SCALE_AMPL);
    }

    private static float clamp(float v, float a, float b) {
        return v < a ? a : (v > b ? b : v);
    }
}
