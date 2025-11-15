package com.akira.noisetint.mixin.bop;

import com.akira.noisetint.common.GenContext;
import com.akira.noisetint.common.Variant;
import biomesoplenty.common.world.TerrainSettings;
import biomesoplenty.common.world.ChunkGeneratorOverworldBOP;
import net.minecraft.world.World;                         // NEW
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Shadow;               // NEW

import java.util.Locale;

@Pseudo
@Mixin(targets = "biomesoplenty.common.world.ChunkGeneratorOverworldBOP", remap = false)
public abstract class MixinChunkGeneratorOverworldBOP_Bias {

    @Invoker("getWeightedTerrainSettings")
    abstract TerrainSettings nb$invokeGetWeightedTerrainSettings(int localX, int localZ, Biome[] biomes);

    @Shadow(remap = false) private World world;          // NEW: to fetch seed

    private static final Logger LOG = LogManager.getLogger("NoisyBiomes-BoP");
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_EDGES = true;

    private static float w5(int di, int dj) {
        return (float)(0.06476162171D / Math.sqrt((di * di + dj * dj) + 0.2D));
    }

    // 4×4 cell center in world coords
    private static int sx(int cX, int lX) { return (cX << 4) + (lX << 2) + 2; }
    private static int sz(int cZ, int lZ) { return (cZ << 4) + (lZ << 2) + 2; }

    // Keep jitter tight. Start with 1; only try 2 if you still see a faint grid.
    private static final int JITTER_RANGE = 1;           // CHANGED

    // Seam-safe jitter in {-1,0,1}, keyed by world 4×4 cell + world seed
    private static int j2(long seed, int cx4, int cz4, int dx, int dz, int salt) {  // CHANGED
        long h = seed
                ^ (long)cx4 * 0x9E3779B97F4A7C15L
                ^ (long)cz4 * 0xC2B2AE3D27D4EB4FL
                ^ (long)dx  * 0x165667B19E3779F9L
                ^ (long)dz  * 0x85EBCA77C2B2AE63L
                ^ (long)salt;
        // mix
        h ^= (h >>> 33); h *= 0xff51afd7ed558ccdl;
        h ^= (h >>> 33); h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        int r = (int)((h >>> 61) & 0x7); // 0..7
        // map roughly 1/4 −1, 1/2 0, 1/4 +1 (reduces bias)
        return (r <= 1) ? -1 : (r >= 6 ? 1 : 0);
    }

    @Redirect(
        method = "populateNoiseArray",
        at = @At(
            value = "INVOKE",
            target = "Lbiomesoplenty/common/world/ChunkGeneratorOverworldBOP;getWeightedTerrainSettings(II[Lnet/minecraft/world/biome/Biome;)Lbiomesoplenty/common/world/TerrainSettings;"
        ),
        remap = false
    )
    private TerrainSettings nb$wrapWeighted(ChunkGeneratorOverworldBOP self,
                                            int localX, int localZ, Biome[] biomes)
    {
        final TerrainSettings ts = nb$invokeGetWeightedTerrainSettings(localX, localZ, biomes);

        // copy
        final TerrainSettings out = new TerrainSettings();
        out.avgHeight = ts.avgHeight;
        out.variationAbove = ts.variationAbove;
        out.variationBelow = ts.variationBelow;
        out.sidewaysNoiseAmount = ts.sidewaysNoiseAmount;
        out.minHeight = ts.minHeight;
        out.maxHeight = ts.maxHeight;
        System.arraycopy(ts.octaveWeights, 0, out.octaveWeights, 0, ts.octaveWeights.length);

        final int[] cc = GenContext.peek();
        if (cc == null) return out;

        final Biome centerBiome = biomes[localX + 2 + (localZ + 2) * 10];
        final boolean centerOnly =
                centerBiome == net.minecraft.init.Biomes.RIVER ||
                centerBiome == net.minecraft.init.Biomes.FROZEN_RIVER ||
                (centerBiome instanceof biomesoplenty.common.biome.overworld.BOPOverworldBiome &&
                 ((biomesoplenty.common.biome.overworld.BOPOverworldBiome) centerBiome).noNeighborTerrainInfuence);

        final int cX = cc[0], cZ = cc[1];
        final int baseX = sx(cX, localX), baseZ = sz(cZ, localZ);

        final int ix = (baseX - 2) >> 2; // 4×4 cell index
        final int iz = (baseZ - 2) >> 2;

        final long seed = this.world.getSeed();          // NEW

        double sumW = 0.0, dW = 0.0, sW = 0.0;
        for (int di = -2; di <= 2; di++) {
            for (int dj = -2; dj <= 2; dj++) {
                final float w = w5(di, dj);

                // seam-safe, seed-keyed jitter inside the neighbor cell
                final int jx = j2(seed, ix + di, iz + dj, di, dj, 0xA1F3);
                final int jz = j2(seed, ix + di, iz + dj, dj, di, 0xB4C7);

                final int nx = ((ix + di) << 2) + 2 + jx;
                final int nz = ((iz + dj) << 2) + 2 + jz;

                dW += w * Variant.depthDelta(nx, nz);
                sW += w * Variant.scaleDelta(nx, nz);
                sumW += w;
            }
        }
        if (sumW != 0.0) { dW /= sumW; sW /= sumW; }

        // bias attenuation for center-only cases
        final double CENTER_ATTENUATION = 0.65;          // CHANGED to what you were testing
        final double depthBias = centerOnly ? dW * CENTER_ATTENUATION : dW;
        final double scaleBias = centerOnly ? sW * CENTER_ATTENUATION : sW;

        // map to TerrainSettings
        out.avgHeight      += 12.0D * depthBias;
        out.variationAbove += 56.0D * scaleBias;
        out.variationBelow += 14.0D * scaleBias;

        if (out.minHeight > out.maxHeight) {
            final double t = out.minHeight; out.minHeight = out.maxHeight; out.maxHeight = t;
        }

        if (DEBUG) {
            // FIXED: proper formatting
            LOG.info(String.format(Locale.ROOT,
                    "[NoisyBiomes-BoP] bias=%s c=(%d,%d) lx=%d lz=%d biome=%s -> sx=%d sz=%d dBias=%.5f sBias=%.5f add(avg,va,vb)=(%.2f,%.2f,%.2f) final(avg,va,vb)=(%.2f,%.2f,%.2f)",
                    (centerOnly ? "WEIGHTED(att)" : "WEIGHTED"),
                    cX, cZ, localX, localZ, centerBiome.getBiomeName(),
                    baseX, baseZ, depthBias, scaleBias,
                    14.0D * depthBias, 64.0D * scaleBias, 16.0D * scaleBias,
                    out.avgHeight, out.variationAbove, out.variationBelow));
        }

        return out;
    }
}
