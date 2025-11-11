package com.akira.noisetint.client;

import com.akira.noisetint.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IRegistryDelegate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.registry.IRegistry;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class ColorTintHandler {

    private static final Logger LOGGER = LogManager.getLogger("NoisyBiomes");
    private static boolean LOGGED = false;
    private static final Set<Block> WRAPPED_BLOCKS = new HashSet<>();
    private static final Set<Block> FALLBACK_BLOCKS = new HashSet<>();
    private static final IBlockColor FALLBACK_BLOCK_COLOR = new FallbackBlockColor();
    @Nullable
    private static IRegistry<ModelResourceLocation, IBakedModel> MODEL_REGISTRY;
    @Nullable
    private static Map<IBlockState, IBakedModel> STATE_MODEL_CACHE;

    private static void logOnce(String msg) {
        if (!LOGGED) {
            LOGGER.info(msg);
            LOGGED = true;
        }
    }

    @SubscribeEvent
    public void onBlockColors(ColorHandlerEvent.Block e) {
        registerBlockColors(e.getBlockColors());
    }

    @SubscribeEvent
    public void onItemColors(ColorHandlerEvent.Item e) {
        // Keep items vanilla to avoid weird UI; no registration needed.
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent e) {
        MODEL_REGISTRY = e.getModelRegistry();
        if (!FALLBACK_BLOCKS.isEmpty()) {
            wrapModelsForBlocks(MODEL_REGISTRY, FALLBACK_BLOCKS);
        }
    }

    public static void registerBlockAndItemColors() {
        BlockColors bc = Minecraft.getMinecraft().getBlockColors();
        registerBlockColors(bc);
    }

    private static void registerBlockColors(BlockColors bc) {
        Map<?, IBlockColor> colorMap;
        try {
            colorMap = ReflectionHelper.getPrivateValue(
                    BlockColors.class,
                    bc,
                    "blockColorMap",
                    "field_186725_a",
                    "mapBlockColors");
        } catch (UnableToAccessFieldException ex) {
            logOnce("Failed to access blockColorMap via reflection; skipping dynamic registration");
            return;
        }

        if (colorMap == null) {
            logOnce("BlockColors.blockColorMap was null; skipping dynamic registration");
            return;
        }

        List<Object> keys = new ArrayList<>(colorMap.keySet());
        Map<Block, IBlockColor> originals = new HashMap<>();
        for (Object key : keys) {
            Block block = unwrapBlockKey(key);
            if (block == null) {
                continue;
            }

            IBlockColor original = colorMap.get(key);
            if (original == null || original instanceof WrappedBlockColor || !WRAPPED_BLOCKS.add(block)) {
                continue;
            }
            originals.put(block, original);
        }

        if (originals.isEmpty()) {
            logOnce("No block color handlers found to wrap");
        } else {
            logOnce("Wrapping " + originals.size() + " block color handlers for noise tinting");

            for (Map.Entry<Block, IBlockColor> entry : originals.entrySet()) {
                Block block = entry.getKey();
                IBlockColor wrapped = new WrappedBlockColor(block, entry.getValue());
                bc.registerBlockColorHandler(wrapped, block);
            }
        }

        List<Block> fallbackTargets = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
            if (block == null || WRAPPED_BLOCKS.contains(block)) {
                continue;
            }

            if (block.getDefaultState().getMaterial() == Material.WATER) {
                continue;
            }

            Object delegate = block.delegate;
            if (delegate != null && colorMap.containsKey(delegate)) {
                // Already has a handler (likely wrapped above).
                continue;
            }

            fallbackTargets.add(block);
        }

        if (!fallbackTargets.isEmpty()) {
            bc.registerBlockColorHandler(FALLBACK_BLOCK_COLOR, fallbackTargets.toArray(new Block[0]));
            WRAPPED_BLOCKS.addAll(fallbackTargets);
            FALLBACK_BLOCKS.addAll(fallbackTargets);
            LOGGER.info("Registered fallback tint handler for {} blocks", fallbackTargets.size());
            if (MODEL_REGISTRY != null) {
                wrapModelsForBlocks(MODEL_REGISTRY, fallbackTargets);
            }
        }
    }

    private static void wrapModelsForBlocks(IRegistry<ModelResourceLocation, IBakedModel> registry, Iterable<Block> blocks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        if (dispatcher == null) {
            return;
        }

        BlockModelShapes shapes = dispatcher.getBlockModelShapes();
        if (shapes == null) {
            return;
        }

        BlockStateMapper mapper = shapes.getBlockStateMapper();
        Map<IBlockState, IBakedModel> stateModels = getStateModelCache(shapes);

        for (Block block : blocks) {
            Map<IBlockState, ModelResourceLocation> variants = mapper.getVariants(block);
            if (variants == null || variants.isEmpty()) {
                continue;
            }

            for (Map.Entry<IBlockState, ModelResourceLocation> entry : variants.entrySet()) {
                ModelResourceLocation mrl = entry.getValue();
                if (mrl == null) {
                    continue;
                }

                IBakedModel model = registry.getObject(mrl);
                if (model == null) {
                    continue;
                }

                IBakedModel tinted = model instanceof TintedBakedModel ? model : new TintedBakedModel(model);
                if (tinted != model) {
                    registry.putObject(mrl, tinted);
                }

                if (stateModels != null) {
                    IBlockState state = entry.getKey();
                    IBakedModel cached = stateModels.get(state);
                    if (cached != null && !(cached instanceof TintedBakedModel)) {
                        stateModels.put(state, tinted);
                    }
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Map<IBlockState, IBakedModel> getStateModelCache(BlockModelShapes shapes) {
        if (STATE_MODEL_CACHE != null) {
            return STATE_MODEL_CACHE;
        }

        try {
            STATE_MODEL_CACHE = ReflectionHelper.getPrivateValue(
                    BlockModelShapes.class,
                    shapes,
                    "stateModels",
                    "bakedModelStore",
                    "field_178124_c",
                    "field_178123_b");
        } catch (UnableToAccessFieldException ignored) {
            // fall through to reflective search
        }

        if (STATE_MODEL_CACHE != null) {
            return STATE_MODEL_CACHE;
        }

        for (Field field : BlockModelShapes.class.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(shapes);
                if (!(value instanceof Map)) {
                    continue;
                }

                Map<?, ?> map = (Map<?, ?>) value;
                if (!map.isEmpty()) {
                    Object sample = map.values().iterator().next();
                    if (sample instanceof IBakedModel) {
                        STATE_MODEL_CACHE = (Map<IBlockState, IBakedModel>) map;
                        return STATE_MODEL_CACHE;
                    }
                } else {
                    String name = field.getName();
                    if ("stateModels".equals(name) ||
                            "bakedModelStore".equals(name) ||
                            "field_178124_c".equals(name) ||
                            "field_178123_b".equals(name)) {
                        STATE_MODEL_CACHE = (Map<IBlockState, IBakedModel>) map;
                        return STATE_MODEL_CACHE;
                    }
                }
            } catch (IllegalAccessException ignored) {
                // try next field
            }
        }

        logOnce("Unable to locate BlockModelShapes state model cache; fallback tinting may be incomplete");
        return null;
    }

    private static class WrappedBlockColor implements IBlockColor {
        private final IBlockColor delegate;
        private final boolean isWater;

        private WrappedBlockColor(Block block, IBlockColor delegate) {
            this.delegate = delegate;
            Material material = block.getDefaultState().getMaterial();
            this.isWater = material == Material.WATER;
        }

        @Override
        public int colorMultiplier(net.minecraft.block.state.IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
            if (world == null || pos == null) {
                return delegate.colorMultiplier(state, world, pos, tintIndex);
            }

            int base = delegate.colorMultiplier(state, world, pos, tintIndex);
            if (base == -1) {
                return base;
            }

            float[] tint = ChunkTintCache.getTint(world, pos);
            if (isWater) {
                return applyWaterTint(base, tint);
            }
            return applyTint(base, tint, false);
        }
    }

    private static class FallbackBlockColor implements IBlockColor {

        @Override
        public int colorMultiplier(net.minecraft.block.state.IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
            if (world == null || pos == null) {
                return 0xFFFFFF;
            }

            net.minecraft.block.state.IBlockState actual = state;
            try {
                actual = state.getActualState(world, pos);
            } catch (Exception ignored) {
                // Chunk might be missing; fall back to the passed state.
            }

            MapColor mapColor = actual.getMapColor(world, pos);
            int base = mapColor != null ? mapColor.colorValue : 0xFFFFFF;

            float[] tint = ChunkTintCache.getTint(world, pos);
            return applyTint(base, tint, true);
        }
    }

    private static int applyTint(int baseColor, float[] tint, boolean boostLowSaturation) {
        float[] hsv = ColorUtil.rgbToHsv(baseColor);
        if (boostLowSaturation) {
            if (hsv[1] < 0.1f) {
                hsv[1] = 0.1f;
            }
            if (hsv[2] < 0.25f) {
                hsv[2] = 0.25f;
            }
        }

        hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0]);
        hsv[1] = ColorUtil.clamp01(hsv[1] * tint[1]);
        hsv[2] = ColorUtil.clamp01(hsv[2] * tint[2]);

        return ColorUtil.hsvToRgbInt(hsv);
    }

    private static int applyWaterTint(int baseColor, float[] tint) {
        float[] hsv = ColorUtil.rgbToHsv(baseColor);
        float hueScale = 0.5f;
        float satScale = 0.5f;
        float valScale = 0.5f;

        hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0] * hueScale);
        hsv[1] = ColorUtil.clamp01(hsv[1] * (1.0f + (tint[1] - 1.0f) * satScale));
        hsv[2] = ColorUtil.clamp01(hsv[2] * (1.0f + (tint[2] - 1.0f) * valScale));

        return ColorUtil.hsvToRgbInt(hsv);
    }

    @Nullable
    private static Block unwrapBlockKey(Object key) {
        if (key instanceof Block) {
            return (Block) key;
        }

        if (key instanceof IRegistryDelegate) {
            Object value = ((IRegistryDelegate<?>) key).get();
            if (value instanceof Block) {
                return (Block) value;
            }
        }

        return null;
    }

    private static class TintedBakedModel implements IBakedModel {
        private final IBakedModel delegate;

        private TintedBakedModel(IBakedModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            List<BakedQuad> original = delegate.getQuads(state, side, rand);
            if (original.isEmpty()) {
                return original;
            }

            boolean mutated = false;
            List<BakedQuad> result = new ArrayList<>(original.size());
            for (BakedQuad quad : original) {
                if (quad.hasTintIndex()) {
                    result.add(quad);
                } else {
                    result.add(withTintIndex(quad, 0));
                    mutated = true;
                }
            }
            return mutated ? result : original;
        }

        @Override
        public boolean isAmbientOcclusion() {
            return delegate.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return delegate.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return delegate.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return delegate.getParticleTexture();
        }

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return delegate.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return delegate.getOverrides();
        }
    }

    private static BakedQuad withTintIndex(BakedQuad quad, int tintIndex) {
        int[] data = quad.getVertexData().clone();
        EnumFacing face = quad.getFace();
        TextureAtlasSprite sprite = quad.getSprite();
        boolean diffuse = quad.shouldApplyDiffuseLighting();
        VertexFormat format = quad.getFormat();
        return new BakedQuad(data, tintIndex, face, sprite, diffuse, format);
    }
}
