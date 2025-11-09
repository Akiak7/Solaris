package com.akira.noisetint.client;

import com.akira.noisetint.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ColorTintHandler {

    private static boolean LOGGED = false;

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
        ItemColors ic = e.getItemColors();
    }

    public static void registerBlockAndItemColors() {
        BlockColors bc = Minecraft.getMinecraft().getBlockColors();
        registerBlockColors(bc);
    }

    private static void registerBlockColors(BlockColors bc) {
        // Build the list of blocks that commonly use biome tinting
        List<Block> grassLike = new ArrayList<>();
        grassLike.add(Blocks.GRASS);
        grassLike.add(Blocks.TALLGRASS);
        grassLike.add(Blocks.DOUBLE_PLANT);
        grassLike.add(Blocks.VINE);

        List<Block> leavesLike = new ArrayList<>();
        leavesLike.add(Blocks.LEAVES);
        leavesLike.add(Blocks.LEAVES2);

        // Water blocks (flowing & still). These accept IBlockColor in 1.12
        List<Block> waterLike = new ArrayList<>();
        waterLike.add(Blocks.WATER);
        waterLike.add(Blocks.FLOWING_WATER);

        IBlockColor grassHandler = new IBlockColor() {
            @Override
            public int colorMultiplier(net.minecraft.block.state.IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
                if (world == null || pos == null) return -1;
                int base = BiomeColorHelper.getGrassColorAtPos(world, pos);
                float[] hsv = ColorUtil.rgbToHsv(base);
                float[] tint = ChunkTintCache.getTint(world, pos); // {hueDeg, satMul, valMul}
                hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0]);
                hsv[1] = ColorUtil.clamp01(hsv[1] * tint[1]);
                hsv[2] = ColorUtil.clamp01(hsv[2] * tint[2]);
                return ColorUtil.hsvToRgbInt(hsv);
            }
        };

        IBlockColor foliageHandler = new IBlockColor() {
            @Override
            public int colorMultiplier(net.minecraft.block.state.IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
                if (world == null || pos == null) return -1;
                int base = BiomeColorHelper.getFoliageColorAtPos(world, pos);
                float[] hsv = ColorUtil.rgbToHsv(base);
                float[] tint = ChunkTintCache.getTint(world, pos);
                hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0]);
                hsv[1] = ColorUtil.clamp01(hsv[1] * tint[1]);
                hsv[2] = ColorUtil.clamp01(hsv[2] * tint[2]);
                return ColorUtil.hsvToRgbInt(hsv);
            }
        };

        IBlockColor waterHandler = new IBlockColor() {
            @Override
            public int colorMultiplier(net.minecraft.block.state.IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
                if (world == null || pos == null) return -1;
                int base = BiomeColorHelper.getWaterColorAtPos(world, pos);
                // Water gets gentler tint
                float[] hsv = ColorUtil.rgbToHsv(base);
                float[] tint = ChunkTintCache.getTint(world, pos);
                float hueScale = 0.5f;   // dampen water hue shift
                float satScale = 0.5f;
                float valScale = 0.5f;
                hsv[0] = ColorUtil.wrapHue(hsv[0] + tint[0] * hueScale);
                hsv[1] = ColorUtil.clamp01(hsv[1] * (1.0f + (tint[1] - 1.0f) * satScale));
                hsv[2] = ColorUtil.clamp01(hsv[2] * (1.0f + (tint[2] - 1.0f) * valScale));
                return ColorUtil.hsvToRgbInt(hsv);
            }
        };

        logOnce("Registering color handlers for grass/leaves/water");

        for (Block b : grassLike) {
            bc.registerBlockColorHandler(grassHandler, b);
        }
        for (Block b : leavesLike) {
            bc.registerBlockColorHandler(foliageHandler, b);
        }
        for (Block b : waterLike) {
            bc.registerBlockColorHandler(waterHandler, b);
        }
    }
}