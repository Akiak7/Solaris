package com.akira.noisetint.mixin;

import com.akira.noisetint.common.Variant;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds tiny, deterministic deltas to base height (depth) and height variation (scale)
 * inside ChunkGeneratorOverworld#generateHeightmap. Chunk coords are inferred from the
 * method args; we sample a low-frequency noise at the chunk center so the bias is coherent.
 */
@Mixin(ChunkGeneratorOverworld.class)
public abstract class MixinChunkGeneratorOverworld {

    @Unique private static final ThreadLocal<int[]> nb$chunkScaled = new ThreadLocal<>();

    // Signature in MCP (stable_39) is generateHeightmap(int x, int z)
    @Inject(method = "generateHeightmap", at = @At("HEAD"))
    private void nb$onHeightmapHead(int xScaled4, int zScaled4, CallbackInfo ci) {
        // x,z arrive scaled by 4; recover chunk coords
        int cx = xScaled4 >> 2;
        int cz = zScaled4 >> 2;
        nb$chunkScaled.set(new int[]{cx, cz});
    }

    @Inject(method = "generateHeightmap", at = @At("RETURN"))
    private void nb$onHeightmapReturn(int xScaled4, int zScaled4, CallbackInfo ci) {
        nb$chunkScaled.remove();
    }

    @Redirect(
        method = "generateHeightmap",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getBaseHeight()F")
    )
    private float nb$biasBaseHeight(Biome biome) {
        float base = biome.getBaseHeight();
        int[] cc = nb$chunkScaled.get();
        if (cc == null) return base;
        // sample at chunk center in block coords; very-low-frequency map means this is sufficient
        int bx = cc[0] * 16 + 8;
        int bz = cc[1] * 16 + 8;
        return base + Variant.depthDelta(bx, bz);
    }

    @Redirect(
        method = "generateHeightmap",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getHeightVariation()F")
    )
    private float nb$biasHeightVariation(Biome biome) {
        float var = biome.getHeightVariation();
        int[] cc = nb$chunkScaled.get();
        if (cc == null) return var;
        int bx = cc[0] * 16 + 8;
        int bz = cc[1] * 16 + 8;
        return var + Variant.scaleDelta(bx, bz);
    }
}
