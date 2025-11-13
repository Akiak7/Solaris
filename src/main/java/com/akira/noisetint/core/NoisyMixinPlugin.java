package com.akira.noisetint.core;

import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class NoisyMixinPlugin implements IMixinConfigPlugin {

    private static boolean isBopLoadedSafely() {
        try {
            return Loader.isModLoaded("biomesoplenty"); // 1.12.x-safe
        } catch (Throwable t) {
            return false;
        }
    }

    @Override public void onLoad(String mixinPackage) { /* no-op: don't classload here */ }

    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Gate *all* BoP mixins by package, not by individual class name
        if (mixinClassName.startsWith("com.akira.noisetint.mixin.bop.")) {
            return isBopLoadedSafely();
        }
        return true; // vanilla/forge mixins always fine
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
