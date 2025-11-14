package com.akira.noisetint.mixin.bop; 
import com.akira.noisetint.common.GenContext; 
import com.akira.noisetint.common.Variant; 
import biomesoplenty.common.world.TerrainSettings; 
import biomesoplenty.common.world.ChunkGeneratorOverworldBOP; 
import net.minecraft.world.biome.Biome; 
import org.spongepowered.asm.mixin.Mixin; 
import org.spongepowered.asm.mixin.Pseudo; 
import org.spongepowered.asm.mixin.injection.At; 
import org.spongepowered.asm.mixin.injection.Redirect; 
import org.spongepowered.asm.mixin.gen.Invoker; 
@Pseudo 
@Mixin(targets = "biomesoplenty.common.world.ChunkGeneratorOverworldBOP", remap = false) 
public abstract class MixinChunkGeneratorOverworldBOP_Bias { // Invoker bridge to the private method 
@Invoker("getWeightedTerrainSettings") 
abstract TerrainSettings nb$invokeGetWeightedTerrainSettings(int localX, int localZ, Biome[] biomes); 
@Redirect(
    method = "populateNoiseArray",
    at = @At(
        value = "INVOKE",
        target = "Lbiomesoplenty/common/world/ChunkGeneratorOverworldBOP;getWeightedTerrainSettings(II[Lnet/minecraft/world/biome/Biome;)Lbiomesoplenty/common/world/TerrainSettings;"
    ),
    remap = false
)
private TerrainSettings nb$wrapWeighted(ChunkGeneratorOverworldBOP self,
                                        int localX, int localZ, Biome[] biomes) {
    // Call original (private) via invoker
    TerrainSettings ts = nb$invokeGetWeightedTerrainSettings(localX, localZ, biomes);

    // Clone to avoid mutating shared settings
    TerrainSettings out = new TerrainSettings();
    out.avgHeight = ts.avgHeight;
    out.variationAbove = ts.variationAbove;
    out.variationBelow = ts.variationBelow;
    out.sidewaysNoiseAmount = ts.sidewaysNoiseAmount;
    out.minHeight = ts.minHeight;
    out.maxHeight = ts.maxHeight;
    System.arraycopy(ts.octaveWeights, 0, out.octaveWeights, 0, ts.octaveWeights.length);

    // --- per-column world coords (NO chunk-center sampling) ---
    int[] cc = GenContext.peek(); // {chunkX, chunkZ} pushed in populateNoiseArray HEAD
    if (cc != null) {
        final int bx = (cc[0] << 4) + localX; // world X for this column
        final int bz = (cc[1] << 4) + localZ; // world Z for this column

        final double dDepth = Variant.depthDelta(bx, bz);
        final double dScale = Variant.scaleDelta(bx, bz);

        // Vanilla->BoP mapping you had is fine; apply per-column now
        out.avgHeight      += 17.0D * dDepth;
        out.variationAbove += 80.0D * dScale;
        out.variationBelow += 20.0D * dScale;

        // Optional: mild guard so we don't flip min/max due to extreme noise
        if (out.minHeight > out.maxHeight) {
            double t = out.minHeight; out.minHeight = out.maxHeight; out.maxHeight = t;
        }
    }
    return out;
}

}