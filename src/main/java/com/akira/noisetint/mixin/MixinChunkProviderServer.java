package com.akira.noisetint.mixin;

import com.akira.noisetint.common.GenContext;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer {

    @Shadow @Final private WorldServer world;

    // Runs for ALL world types/generators
    @Inject(method = "provideChunk", at = @At("HEAD"))
    private void nb$onProvideChunkHead(int cx, int cz, CallbackInfoReturnable<Chunk> cir) {
        // Only bias the overworld; drop this if you want all dims affected
        if (world != null && world.provider.getDimension() == 0) {
            GenContext.push(cx,cz);
        }
    }

    @Inject(method = "provideChunk", at = @At("RETURN"))
    private void nb$onProvideChunkReturn(int cx, int cz, CallbackInfoReturnable<Chunk> cir) {
        GenContext.pop();
    }
}
