package dev.anvilcraft.lib.v2.sync.network;

import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AnvilLibSync.MOD_ID)
public class AnvilLibSyncNetwork {
    public static final String NETWORK_VERSION = "2";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        NetworkRegistrar.register(registrar, AnvilLibSync.MOD_ID);
    }
}
