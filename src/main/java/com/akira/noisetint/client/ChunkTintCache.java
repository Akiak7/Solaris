package com.akira.noisetint.client;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches per-chunk low-res tint samples (9x9) with bilinear interpolation.
 * Each node stores {hueDeg, satMul, valMul}.
 */
@SideOnly(Side.CLIENT)
public class ChunkTintCache {

    private static final int GRID = 9; // 9x9 nodes
    private static final int STEP = 2; // every 2 blocks
    private static final Map<Long, float[][][]> CACHE = new ConcurrentHashMap<>();

    public static void clearAll() {
        CACHE.clear();
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload e) {
        if (e.getWorld().isRemote) {
            ChunkPos cp = e.getChunk().getPos();
            CACHE.remove(ChunkPos.asLong(cp.x, cp.z)); // 1.12: static asLong(int x, int z)
        }
    }

    public static float[] getTint(IBlockAccess world, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        long key = ChunkPos.asLong(cx, cz);
        float[][][] grid = CACHE.get(key);
        if (grid == null) {
            grid = buildGridForChunk(cx, cz);
            CACHE.put(key, grid);
        }
        // local coords
        int lx = pos.getX() & 15;
        int lz = pos.getZ() & 15;
        // indices in grid (0..16 mapped into 0..(GRID-1))
        float fx = lx / (float) STEP;
        float fz = lz / (float) STEP;
        int x0 = Math.min((int)Math.floor(fx), GRID - 2);
        int z0 = Math.min((int)Math.floor(fz), GRID - 2);
        float tx = fx - x0;
        float tz = fz - z0;

        float[] c00 = grid[x0][z0];
        float[] c10 = grid[x0+1][z0];
        float[] c01 = grid[x0][z0+1];
        float[] c11 = grid[x0+1][z0+1];

        // Bilinear interpolation for sat/val multipliers (linear)
        float sat = lerp2(c00[1], c10[1], c01[1], c11[1], tx, tz);
        float val = lerp2(c00[2], c10[2], c01[2], c11[2], tx, tz);

        // Hue needs circular interpolation. We'll approximate by shortest-arc blend in degrees.
        float hA = c00[0];
        float hB = lerp2Angle(c00[0], c10[0], c01[0], c11[0], tx, tz);

        return new float[]{hB, sat, val};
    }

    private static float[][][] buildGridForChunk(int cx, int cz) {
        float[][][] grid = new float[GRID][GRID][3];
        int worldX0 = (cx << 4);
        int worldZ0 = (cz << 4);
        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                int wx = worldX0 + gx * STEP;
                int wz = worldZ0 + gz * STEP;
                float[] t = NoiseField.sampleTint(wx, wz);
                grid[gx][gz][0] = t[0];
                grid[gx][gz][1] = t[1];
                grid[gx][gz][2] = t[2];
            }
        }
        return grid;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float lerp2(float c00, float c10, float c01, float c11, float tx, float tz) {
        float a = lerp(c00, c10, tx);
        float b = lerp(c01, c11, tx);
        return lerp(a, b, tz);
    }

    private static float lerpAngle(float aDeg, float bDeg, float t) {
        float diff = ((bDeg - aDeg + 540f) % 360f) - 180f;
        return (aDeg + diff * t + 360f) % 360f;
    }

    private static float lerp2Angle(float c00, float c10, float c01, float c11, float tx, float tz) {
        float a = lerpAngle(c00, c10, tx);
        float b = lerpAngle(c01, c11, tx);
        return lerpAngle(a, b, tz);
    }
}