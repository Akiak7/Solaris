package com.akira.noisetint.mixin.bop;

import com.akira.noisetint.common.GenContext;
import com.akira.noisetint.common.Variant;
import biomesoplenty.common.world.TerrainSettings;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "biomesoplenty.common.world.ChunkGeneratorOverworldBOP", remap = false)
public abstract class MixinChunkGeneratorOverworldBOP_Bias {

    // We shadow the original so we can call it without depending on BoP at compile time
    @Shadow
    private TerrainSettings getWeightedTerrainSettings(int localX, int localZ, Biome[] biomes) { throw new AssertionError(); }

    /**
     * Redirect the call inside populateNoiseArray(...) which does:
     *   TerrainSettings settings = this.getWeightedTerrainSettings(ix, iz, biomes);
     * We call the original, clone it, then add our small deterministic deltas.
     */
    @Redirect(
        method = "populateNoiseArray",
        at = @At(
            value = "INVOKE",
            target = "Lbiomesoplenty/common/world/ChunkGeneratorOverworldBOP;getWeightedTerrainSettings(II[Lnet/minecraft/world/biome/Biome;)Lbiomesoplenty/common/world/TerrainSettings;"
        ),
        remap = false
    )
    private TerrainSettings nb$wrapWeighted(Object self, int localX, int localZ, Biome[] biomes) {
        TerrainSettings ts = this.getWeightedTerrainSettings(localX, localZ, biomes);

        // Always clone to avoid mutating shared per-biome settings (rivers / cached map)
        TerrainSettings out = new TerrainSettings();
        out.avgHeight         = ts.avgHeight;
        out.variationAbove    = ts.variationAbove;
        out.variationBelow    = ts.variationBelow;
        out.sidewaysNoiseAmount = ts.sidewaysNoiseAmount;
        out.minHeight         = ts.minHeight;
        out.maxHeight         = ts.maxHeight;
        System.arraycopy(ts.octaveWeights, 0, out.octaveWeights, 0, ts.octaveWeights.length);

        // Sample once per chunk (center) for coherence
        int[] cc = GenContext.peek(); // {cx, cz}
        if (cc != null) {
            int bx = cc[0] * 16 + 8;
            int bz = cc[1] * 16 + 8;

            double dDepth = Variant.depthDelta(bx, bz); // vanilla units (Biome baseHeight)
            double dScale = Variant.scaleDelta(bx, bz); // vanilla units (Biome heightVariation)

            // Map vanilla -> BoP parameters:
            // avgHeight      = 65 + 17 * baseHeight
            // variationAbove = 7  + 80 * heightVariation
            // variationBelow = 4  + 20 * heightVariation
            out.avgHeight      += 17.0D * dDepth;
            out.variationAbove += 80.0D * dScale;
            out.variationBelow += 20.0D * dScale;
        }

        return out;
    }
}
