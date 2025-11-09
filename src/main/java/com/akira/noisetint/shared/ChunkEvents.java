package com.akira.noisetint.shared;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.event.world.ChunkEvent;

public class ChunkEvents {
    @SubscribeEvent
    public void onLoad(ChunkEvent.Load e) {
        // placeholder if you want to seed per-chunk later (server or client)
    }
}