package com.akira.noisetint.client;

import com.akira.noisetint.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IRegistryDelegate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class ColorTintHandler {

    private static boolean LOGGED = false;
    private static final Set<Block> WRAPPED_BLOCKS = new HashSet<>();

    private static void logOnce(String msg) {
        if (!LOGGED) {
            org.apache.logging.log4j.LogManager.getLogger("NoisyBiomes").info(msg);
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
            return;
        }

        logOnce("Wrapping " + originals.size() + " block color handlers for noise tinting");

        for (Map.Entry<Block, IBlockColor> entry : originals.entrySet()) {
            Block block = entry.getKey();
            IBlockColor wrapped = new WrappedBlockColor(block, entry.getValue());
            bc.registerBlockColorHandler(wrapped, block);
        }
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

            float[] hsv = ColorUtil.rgbToHsv(base);
            float[] tint = ChunkTintCache.getTint(world, pos);

            if (isWater) {
                float hueScale = 0.5f;
                float satScale = 0.5f;
                float valScale = 0.5f;
                hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0] * hueScale);
                hsv[1] = ColorUtil.clamp01(hsv[1] * (1.0f + (tint[1] - 1.0f) * satScale));
                hsv[2] = ColorUtil.clamp01(hsv[2] * (1.0f + (tint[2] - 1.0f) * valScale));
            } else {
                hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0]);
                hsv[1] = ColorUtil.clamp01(hsv[1] * tint[1]);
                hsv[2] = ColorUtil.clamp01(hsv[2] * tint[2]);
            }

            return ColorUtil.hsvToRgbInt(hsv);
        }
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
}
