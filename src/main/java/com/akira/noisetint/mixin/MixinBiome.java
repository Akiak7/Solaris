package com.akira.noisetint.mixin;

import com.akira.noisetint.common.GenContext;
import com.akira.noisetint.common.Variant;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class MixinBiome {

    @Inject(method = "getBaseHeight", at = @At("RETURN"), cancellable = true)
    private void nb$biasBaseHeight(CallbackInfoReturnable<Float> cir) {
        int[] cc = GenContext.peek();
        if (cc != null) {
            int bx = (cc[0] << 4) + 8;
            int bz = (cc[1] << 4) + 8;
            cir.setReturnValue(cir.getReturnValueF() + Variant.depthDelta(bx, bz));
        }
    }

    @Inject(method = "getHeightVariation", at = @At("RETURN"), cancellable = true)
    private void nb$biasHeightVariation(CallbackInfoReturnable<Float> cir) {
        int[] cc = GenContext.peek();
        if (cc != null) {
            int bx = (cc[0] << 4) + 8;
            int bz = (cc[1] << 4) + 8;
            cir.setReturnValue(cir.getReturnValueF() + Variant.scaleDelta(bx, bz));
        }
    }
}
