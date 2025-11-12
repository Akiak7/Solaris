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

@Mixin(value = ChunkGeneratorOverworld.class, remap = false)
public abstract class MixinChunkGeneratorOverworld {

    @Unique private static final ThreadLocal<int[]> nb$chunkScaled = new ThreadLocal<>();

    // Your runtime’s method: generateHeightmap(int xScaled4, int zScaled4, int something)
    @Inject(method = "generateHeightmap(III)V", at = @At("HEAD"))
    private void nb$onHeightmapHead(int xScaled4, int zScaled4, int ignored, CallbackInfo ci) {
        // x,z arrive scaled by 4; recover chunk coords
        nb$chunkScaled.set(new int[]{ xScaled4 >> 2, zScaled4 >> 2 });
    }

    @Inject(method = "generateHeightmap(III)V", at = @At("RETURN"))
    private void nb$onHeightmapReturn(int xScaled4, int zScaled4, int ignored, CallbackInfo ci) {
        nb$chunkScaled.remove();
    }

    @Redirect(
        method = "generateHeightmap(III)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getBaseHeight()F")
    )
    private float nb$biasBaseHeight(Biome biome) {
        float base = biome.getBaseHeight();
        int[] cc = nb$chunkScaled.get();
        if (cc == null) return base;
        int bx = cc[0] * 16 + 8;
        int bz = cc[1] * 16 + 8;
        return base + Variant.depthDelta(bx, bz);
    }

    @Redirect(
        method = "generateHeightmap(III)V",
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
