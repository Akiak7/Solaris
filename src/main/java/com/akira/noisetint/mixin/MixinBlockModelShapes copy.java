package com.akira.noisetint.mixin;

import com.akira.noisetint.client.TintNoise;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public abstract class MixinBlockColors {
    // colorMultiplier(IBlockState, IBlockAccess, BlockPos, int) -> int
    @Inject(
        method = "colorMultiplier(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;I)I",
        at = @At("RETURN"),
        cancellable = true
    )
    private void nb$afterColor(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex,
                               CallbackInfoReturnable<Integer> cir) {
        if (world == null || pos == null) return;                // GUI/item contexts
        if (!(state.getBlock() instanceof BlockLeaves)) return;  // only touch leaves
        int c = cir.getReturnValue();                            // base from original handler
        if (c == -1) return;                                     // layer not tinted
        cir.setReturnValue(TintNoise.jitterHSB(c, pos.getX(), pos.getZ()));
    }
}
