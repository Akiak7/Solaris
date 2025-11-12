package com.akira.noisetint.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.ForgeRegistries; // 1.12 path
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class ModelTintEnforcer {

    private boolean rewrappedOnce = false;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ModelTintEnforcer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onModelBake(ModelBakeEvent evt) {
        int total = enforceOnRegistry(evt.getModelRegistry(), "bake");
        LogManager.getLogger("NoisyBiomes").info("ModelTintEnforcer: patched {} leaf models at bake", total);
    }

@SubscribeEvent
public void onClientTick(TickEvent.ClientTickEvent evt) {
    if (evt.phase != TickEvent.Phase.END || rewrappedOnce) return;
    if (Minecraft.getMinecraft().world == null) return;

    ModelManager mm = Minecraft.getMinecraft()
        .getBlockRendererDispatcher()
        .getBlockModelShapes()
        .getModelManager();

    IRegistry<ModelResourceLocation, IBakedModel> reg = getModelRegistrySafe(mm);
    if (reg != null) {
        int total = enforceOnRegistry(reg, "first-tick");
        LogManager.getLogger("NoisyBiomes").info("ModelTintEnforcer: patched {} leaf models on first tick", total);
    } else {
        LogManager.getLogger("NoisyBiomes").warn("ModelTintEnforcer: no access to ModelManager#modelRegistry; skipping post-tick rewrap");
    }
    rewrappedOnce = true;
}

@SuppressWarnings("unchecked")
private static IRegistry<ModelResourceLocation, IBakedModel> getModelRegistrySafe(ModelManager mm) {
    try {
        // Try MCP then a few common SRG variants; if none match, return null
        return (IRegistry<ModelResourceLocation, IBakedModel>) net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(
            ModelManager.class, mm,
            "modelRegistry",       // MCP
            "field_110617_f",      // SRG variant A
            "f_110617_f",          // SRG variant B (RFG sometimes)
            "field_178092_c"       // old fallback
        );
    } catch (Throwable t) {
        return null;
    }
}

    private int enforceOnRegistry(IRegistry<ModelResourceLocation, IBakedModel> reg, String phase) {
        // Build allowlist of leaf blocks from all mods
        Set<ResourceLocation> leafBlocks = new HashSet<>();
        for (Block b : ForgeRegistries.BLOCKS) {
            if (b == null) continue;
            if (b instanceof BlockLeaves || b.getDefaultState().getMaterial() == Material.LEAVES) {
                ResourceLocation rl = b.getRegistryName();
                if (rl != null) leafBlocks.add(rl);
            }
        }

        int patched = 0;

        // A) Patch entries whose MRL path matches any leaf block registry name (domain:path)
        Set<String> leafPathKeys = leafBlocks.stream()
                .map(rl -> rl.getNamespace() + ":" + rl.getPath())
                .collect(Collectors.toSet());

        for (ModelResourceLocation key : reg.getKeys()) {
            String pathKey = key.getNamespace() + ":" + key.getPath();
            if (!leafPathKeys.contains(pathKey)) continue;
            IBakedModel m = reg.getObject(key);
            if (m == null || m instanceof TintEnforcingModel) continue;
            reg.putObject(key, new TintEnforcingModel(m));
            patched++;
        }

        // B) Robust: enumerate all valid states for each leaf block and patch those MRLs explicitly
        for (ResourceLocation rl : leafBlocks) {
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null) continue;

            for (IBlockState state : block.getBlockState().getValidStates()) {
                ModelResourceLocation mrl = new ModelResourceLocation(rl, variantString(state));
                IBakedModel m = reg.getObject(mrl);
                if (m == null || m instanceof TintEnforcingModel) continue;
                reg.putObject(mrl, new TintEnforcingModel(m));
                patched++;
            }
        }

        LogManager.getLogger("NoisyBiomes").debug("ModelTintEnforcer [{}]: patched {}", phase, patched);
        return patched;
    }

    /** Vanilla-style variant string from state (no fragile generics). */
    private static String variantString(IBlockState state) {
        return state.getProperties().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getName()))
                .map(e -> {
                    IProperty prop = e.getKey();
                    Comparable val = e.getValue();
                    return prop.getName() + "=" + prop.getName(val);
                })
                .collect(Collectors.joining(","));
    }

    /** Wraps a model and forces tintIndex=0 on any quad that had -1. */
    public static class TintEnforcingModel implements IBakedModel {
        private final IBakedModel delegate;

        TintEnforcingModel(IBakedModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
            List<BakedQuad> src = delegate.getQuads(state, side, rand);
            if (src.isEmpty()) return src;

            boolean needsPatch = false;
            for (BakedQuad q : src) {
                if (q.getTintIndex() == -1) { needsPatch = true; break; }
            }
            if (!needsPatch) return src;

            List<BakedQuad> out = new ArrayList<>(src.size());
            for (BakedQuad q : src) {
                if (q.getTintIndex() != -1) {
                    out.add(q);
                } else {
                    out.add(new BakedQuad(
                            q.getVertexData().clone(),
                            0, // force tintIndex=0
                            q.getFace(),
                            q.getSprite(),
                            q.shouldApplyDiffuseLighting(),
                            q.getFormat()
                    ));
                }
            }
            return out;
        }

        // delegate rest
        @Override public boolean isAmbientOcclusion() { return delegate.isAmbientOcclusion(); }
        @Override public boolean isGui3d() { return delegate.isGui3d(); }
        @Override public boolean isBuiltInRenderer() { return delegate.isBuiltInRenderer(); }
        @Override public TextureAtlasSprite getParticleTexture() { return delegate.getParticleTexture(); }
        @Override public ItemCameraTransforms getItemCameraTransforms() { return delegate.getItemCameraTransforms(); }
        @Override public net.minecraft.client.renderer.block.model.ItemOverrideList getOverrides() { return delegate.getOverrides(); }
    }
}
