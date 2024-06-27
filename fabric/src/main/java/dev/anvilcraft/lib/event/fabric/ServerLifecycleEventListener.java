package dev.anvilcraft.lib.event.fabric;

import dev.anvilcraft.lib.util.fabric.ServerHooks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class ServerLifecycleEventListener {
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEventListener::serverStarted);
    }

    private static void serverStarted(MinecraftServer server) {
        ServerHooks.setServer(server);
    }
}
