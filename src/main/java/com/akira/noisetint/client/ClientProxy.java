package com.akira.noisetint.client;

import com.akira.noisetint.CommonProxy;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    @Override
public void preInit() {
    MinecraftForge.EVENT_BUS.register(new FogHandler());
    MinecraftForge.EVENT_BUS.register(new ChunkTintCache());
    MinecraftForge.EVENT_BUS.register(new WorldHooks());
    MinecraftForge.EVENT_BUS.register(new ColorTintHandler());
    //ModelTintEnforcer.register();
}

@Override
public void init() {
    ColorTintHandler.registerBlockAndItemColors();
}

}