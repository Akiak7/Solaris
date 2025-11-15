package com.akira.noisetint.mixin;

import com.akira.noisetint.common.Variant;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

@Pseudo
@Mixin(net.minecraft.world.gen.ChunkGeneratorOverworld.class)
public abstract class MixinChunkGeneratorOverworld_Bias {
    private static final Logger LOG = LogManager.getLogger("NoisyBiomes-Bias");
    private static final boolean DEBUG = false;

    private static int toWorldX(int x, int k)      { return ((x + k + 2) << 2) + 2; }
    private static int toWorldZ(int z, int l, int j1){ return ((z + l + j1 + 2) << 2) + 2; }

    @Redirect(method = "generateHeightmap",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getBaseHeight()F"),
              require = 0)
    private float nb$biasBase(Biome biome,
                              @Local(index = 1) int x, @Local(index = 3) int z,
                              @Local(index = 8) int k, @Local(index = 9) int l,
                              @Local(index = 15) int j1) {
        final int wx = toWorldX(x, k);
        final int wz = toWorldZ(z, l, j1);
        if (DEBUG) LOG.info("[BASE] x={} z={} k={} l={} j1={} -> wx={} wz={} biome={}", x,z,k,l,j1,wx,wz,biome.getBiomeName());
        float out = biome.getBaseHeight() + Variant.depthDelta(wx, wz);
        return out < -2f ? -2f : (out > 2f ? 2f : out);
    }

    @Redirect(method = "generateHeightmap",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getHeightVariation()F"),
              require = 0)
    private float nb$biasVar(Biome biome,
                             @Local(index = 1) int x, @Local(index = 3) int z,
                             @Local(index = 8) int k, @Local(index = 9) int l,
                             @Local(index = 15) int j1) {
        final int wx = toWorldX(x, k);
        final int wz = toWorldZ(z, l, j1);
        if (DEBUG) LOG.info("[VAR ] x={} z={} k={} l={} j1={} -> wx={} wz={} baseH={} varH={}",
                x,z,k,l,j1,wx,wz,biome.getBaseHeight(), biome.getHeightVariation());
        float out = biome.getHeightVariation() + Variant.scaleDelta(wx, wz);
        return out < 0f ? 0f : (out > 2f ? 2f : out);
    }
}

