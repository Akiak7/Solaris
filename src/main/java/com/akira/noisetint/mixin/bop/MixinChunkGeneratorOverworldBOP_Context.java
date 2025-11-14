package com.akira.noisetint.mixin.bop;

import com.akira.noisetint.common.GenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "biomesoplenty.common.world.ChunkGeneratorOverworldBOP", remap = false)
public abstract class MixinChunkGeneratorOverworldBOP_Context {

    @Inject(method = "populateNoiseArray", at = @At("HEAD"), remap = false)
    private void nb$pushCtx(int chunkX, int chunkZ, CallbackInfo ci) {
        GenContext.push(chunkX, chunkZ);
    }

    @Inject(method = "populateNoiseArray", at = @At("RETURN"), remap = false)
    private void nb$popCtx(int chunkX, int chunkZ, CallbackInfo ci) {
        GenContext.pop();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
private void nb$initLog(CallbackInfo ci) {
    System.out.println("[NoisyBiomes] BoP ChunkGenerator mixin active");
}
}
