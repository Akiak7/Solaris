package com.akira.noisetint.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer {

    @Unique private static final Logger NB_LOG = LogManager.getLogger("NoisyBiomes");
    @Unique private static final ThreadLocal<IBlockState> NB$STATE = new ThreadLocal<>();
    @Unique private static boolean NB_LOGGED = false;

    /**
     * Capture the IBlockState passed into IBakedModel#getQuads(state, face, seed)
     * for whatever method in BlockModelRenderer is invoking it.
     */
    @ModifyArg(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/IBakedModel;getQuads(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;"
        ),
        index = 0
    )
    private IBlockState nb$captureState(IBlockState state) {
        NB$STATE.set(state);
        if (!NB_LOGGED) { NB_LOG.info("Renderer mixin ACTIVE (capturing state via getQuads)"); NB_LOGGED = true; }
        return state;
    }

    /**
     * Force leaves quads that lack a tint layer (tintIndex==-1) to behave as if tintIndex==0.
     * This compels Minecraft to query BlockColors, letting our HSV tint code run.
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/BakedQuad;getTintIndex()I"
        )
    )
    private int nb$forceLeafTint(BakedQuad quad) {
        int idx = quad.getTintIndex();
        if (idx != -1) return idx;

        IBlockState st = NB$STATE.get();
        if (st == null) return -1;

        Block b = st.getBlock();
        boolean isLeaves = (b instanceof BlockLeaves) || st.getMaterial() == Material.LEAVES;
        return isLeaves ? 0 : -1;
    }
}
