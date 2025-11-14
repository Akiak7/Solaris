// src/main/java/com/akira/noisetint/core/NoisyCoremod.java
package com.akira.noisetint.core;

import zone.rong.mixinbooter.IEarlyMixinLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class NoisyCoremod implements IEarlyMixinLoader, IFMLLoadingPlugin {

    @Override
    public List<String> getMixinConfigs() {
        // Load both; your NoisyMixinPlugin will gate the BoP ones safely
        return Arrays.asList(
            "mixins.noisybiomes.vanilla.json",
            "mixins.noisybiomes.bop.json"
        );
    }

    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}
