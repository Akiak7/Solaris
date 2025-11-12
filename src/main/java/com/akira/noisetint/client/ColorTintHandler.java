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

// imports you’ll need:
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.registry.ForgeRegistries; // 1.12 path
import net.minecraft.block.BlockLeaves;
import net.minecraft.world.biome.BiomeColorHelper;

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
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
    // Map<?, IBlockColor> colorMap;
    // try {
    //     colorMap = ReflectionHelper.getPrivateValue(
    //             BlockColors.class, bc,
    //             "blockColorMap", "field_186725_a", "mapBlockColors");
    // } catch (UnableToAccessFieldException ex) {
    //     logOnce("Failed to access blockColorMap via reflection; skipping dynamic registration");
    //     return;
    // }
    // if (colorMap == null) {
    //     logOnce("BlockColors.blockColorMap was null; skipping dynamic registration");
    //     return;
    // }

    // // 1) Wrap existing handlers (run last so everyone else is already in)
    // int wrappedExisting = 0;
    // for (Object key : new ArrayList<>(colorMap.keySet())) {
    //     Block block = unwrapBlockKey(key);
    //     if (block == null) continue;
    //     IBlockColor original = colorMap.get(key);
    //     if (original == null || original instanceof WrappedBlockColor || !WRAPPED_BLOCKS.add(block)) continue;
    //     bc.registerBlockColorHandler(new WrappedBlockColor(block, original), block);
    //     wrappedExisting++;
    // }

    // // 2) Force-register for every leaves block (even if there was no delegate)
    // int forcedLeaves = 0;
    // for (Block b : ForgeRegistries.BLOCKS) {
    //     if (b == null) continue;
    //     final Material m = b.getDefaultState().getMaterial();
    //     if (m == Material.LEAVES || b instanceof BlockLeaves) {
    //         if (WRAPPED_BLOCKS.add(b)) {
    //             // may be null; WrappedBlockColor will fallback to foliage colormap
    //             IBlockColor delegate = colorMap.get(b);
    //             bc.registerBlockColorHandler(new WrappedBlockColor(b, delegate), b);
    //             forcedLeaves++;
    //         }
    //     }
    // }

    // logOnce("Wrapped " + wrappedExisting + " existing handlers, forced leaves handlers for " + forcedLeaves + " blocks");
}


private static class WrappedBlockColor implements IBlockColor {
    @Nullable private final IBlockColor delegate;
    private final boolean isWater;
    private final boolean isLeaves;

    private WrappedBlockColor(Block block, @Nullable IBlockColor delegate) {
        this.delegate = delegate;
        Material material = block.getDefaultState().getMaterial();
        this.isWater = material == Material.WATER;
        this.isLeaves = (material == Material.LEAVES) || (block instanceof BlockLeaves);
    }

    @Override
    public int colorMultiplier(net.minecraft.block.state.IBlockState state,
                               @Nullable IBlockAccess world,
                               @Nullable BlockPos pos,
                               int tintIndex) {
        if (world == null || pos == null) {
            return delegate != null ? delegate.colorMultiplier(state, world, pos, tintIndex) : -1;
        }

// TEMP DEBUG: prove BlockColors path hits for BoP leaves
if (isLeaves && state != null) {
    net.minecraft.util.ResourceLocation rl = state.getBlock().getRegistryName();
    if (rl != null && "biomesoplenty".equals(rl.getNamespace())) {
        org.apache.logging.log4j.LogManager.getLogger("NoisyBiomes")
            .info("DEBUG: BoP leaf tint HIT for " + rl + " at " + pos);
        return 0xFFFF00FF; // magenta
    }
}

        int base = (delegate != null) ? delegate.colorMultiplier(state, world, pos, tintIndex) : -1;

        // If the delegate says "no tint" (-1), try a sensible fallback for leaves
// If the delegate says "no tint" (-1), compute foliage base color for any leaf tint layer
if (base == -1 && isLeaves) {
    base = BiomeColorHelper.getFoliageColorAtPos(world, pos);
}

        // Still no base? Then there is no tint layer on this quad; nothing we can do here.
        if (base == -1) return -1;

        float[] hsv = ColorUtil.rgbToHsv(base);
        float[] tint = ChunkTintCache.getTint(world, pos);

        if (isWater) {
            float hueScale = 0.5f, satScale = 0.5f, valScale = 0.5f;
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
