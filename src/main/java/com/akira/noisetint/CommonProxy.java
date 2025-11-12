// com/akira/noisetint/CommonProxy.java
package com.akira.noisetint;

import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new com.akira.noisetint.server.WorldHooksServer());
    }
    public void init() {}
}
