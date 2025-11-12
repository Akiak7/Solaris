package com.akira.noisetint;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = NoisyBiomes.MODID, name = "Noisy Biomes", version = "1.0.0", acceptedMinecraftVersions = "[1.12,1.13)")
public class NoisyBiomes {
    public static final String MODID = "noisybiomes";

    @SidedProxy(clientSide = "com.akira.noisetint.client.ClientProxy", serverSide = "com.akira.noisetint.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        proxy.preInit();
            try {
        org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.noisybiomes.json");
        org.apache.logging.log4j.LogManager.getLogger("NoisyBiomes")
            .info("Injected mixin config programmatically");
    } catch (Throwable t) {
        org.apache.logging.log4j.LogManager.getLogger("NoisyBiomes")
            .error("Failed to inject mixin config", t);
    }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        proxy.init();
        // Register shared event buses if any
        MinecraftForge.EVENT_BUS.register(new com.akira.noisetint.shared.ChunkEvents());
    }
}