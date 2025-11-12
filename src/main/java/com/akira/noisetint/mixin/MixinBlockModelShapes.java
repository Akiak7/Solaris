package com.akira.noisetint.mixin;

import com.akira.noisetint.client.TintModelRuntimeWrapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelShapes.class)
public abstract class MixinBlockModelShapes {

    @Inject(method = "getModelForState", at = @At("RETURN"), cancellable = true)
    private void nb$wrapLeavesModel(IBlockState state, CallbackInfoReturnable<IBakedModel> cir) {
        IBakedModel original = cir.getReturnValue();
        IBakedModel wrapped = TintModelRuntimeWrapper.wrapIfLeaves(state, original);
        if (wrapped != original) {
            cir.setReturnValue(wrapped);
        }
    }
}
