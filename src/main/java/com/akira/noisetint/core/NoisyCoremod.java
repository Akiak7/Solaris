package com.akira.noisetint.core;

import zone.rong.mixinbooter.IEarlyMixinLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class NoisyCoremod implements IEarlyMixinLoader, IFMLLoadingPlugin {

    // Register ONLY the config that targets vanilla/forge here
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.noisybiomes.vanilla.json");
    }

    // ---- IFMLLoadingPlugin stubs ----
    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { /* no-op */ }
    @Override public String getAccessTransformerClass() { return null; }
}
