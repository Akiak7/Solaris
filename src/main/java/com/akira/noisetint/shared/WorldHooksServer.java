package com.akira.noisetint.server;

import com.akira.noisetint.common.NoiseFieldCommon;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class WorldHooksServer {
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (!e.getWorld().isRemote && e.getWorld().provider.getDimension() == 0) {
            NoiseFieldCommon.reseed(e.getWorld().getSeed());
        }
    }
}
