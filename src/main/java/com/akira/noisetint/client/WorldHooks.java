package com.akira.noisetint.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WorldHooks {
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
    if (e.getWorld().isRemote) {
        NoiseField.reseed(e.getWorld().getSeed()); // <- use the event world
        ChunkTintCache.clearAll();
        FogHandler.reset();
    }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote) {
            ChunkTintCache.clearAll();
            FogHandler.reset();
        }
    }
}