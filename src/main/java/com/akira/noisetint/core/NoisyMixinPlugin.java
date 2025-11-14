package com.akira.noisetint.core;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class NoisyMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOG = LogManager.getLogger("NoisyMixinPlugin");
    private static final String BOP_PROBE_RESOURCE =
            "biomesoplenty/common/world/ChunkGeneratorOverworldBOP.class";

    private static boolean isBopPresent() {
        // Primary: ask FML
        boolean fml = false;
        try { fml = Loader.isModLoaded("biomesoplenty"); } catch (Throwable ignored) {}
        if (fml) return true;

        // Fallback: look for the class on the Launch classpath (no classloading!)
        try {
            ClassLoader cl = net.minecraft.launchwrapper.Launch.classLoader;
            return cl.getResource(BOP_PROBE_RESOURCE) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override public void onLoad(String mixinPackage) { /* no-op */ }
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean isBopMixin = mixinClassName.startsWith("com.akira.noisetint.mixin.bop.");
        if (!isBopMixin) return true;

        boolean present = isBopPresent();
        LOG.info("[NoisyMixinPlugin] BoP present={} -> apply {} to {}", present, mixinClassName, targetClassName);
        return present;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String t, ClassNode n, String m, IMixinInfo i) {}
    @Override public void postApply(String t, ClassNode n, String m, IMixinInfo i) {}
}
