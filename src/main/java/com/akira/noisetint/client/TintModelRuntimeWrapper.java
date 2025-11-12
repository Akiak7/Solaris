package com.akira.noisetint.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Wraps baked models at runtime so leaf quads always have a tint layer. */
public final class TintModelRuntimeWrapper {
    // Cache: original -> wrapped (identity-based; IBakedModel identity is stable)
    private static final Map<IBakedModel, IBakedModel> CACHE = new IdentityHashMap<>();

    private TintModelRuntimeWrapper() {}

    public static IBakedModel wrapIfLeaves(IBlockState state, IBakedModel model) {
        if (model == null || state == null) return model;

        final Block b = state.getBlock();
        if (!(b instanceof BlockLeaves) && b.getDefaultState().getMaterial() != Material.LEAVES) {
            return model;
        }

        // If we’ve already wrapped this baked model instance, return cached wrapper.
        IBakedModel cached = CACHE.get(model);
        if (cached != null) return cached;

        // Quick probe: if any quad on the general layer lacks tint, we need a wrapper.
        // (Using side=null is fine; if those quads are tinted, the directional ones usually are too.)
        List<BakedQuad> quads = model.getQuads(state, null, 0L);
        boolean needsPatch = false;
        for (BakedQuad q : quads) {
            if (q.getTintIndex() == -1) { needsPatch = true; break; }
        }
        if (!needsPatch) {
            // Still cache a no-op mapping to avoid re-probing
            CACHE.put(model, model);
            return model;
        }

        IBakedModel wrapped = new ModelTintEnforcer.TintEnforcingModel(model);
        CACHE.put(model, wrapped);
        return wrapped;
    }
}
