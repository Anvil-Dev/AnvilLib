package dev.anvilcraft.lib.v2.space_select.network;

import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AnvilLibSpaceSelect.MOD_ID)
public class AnvilLibSpaceSelectNetwork {
    public static final String NETWORK_VERSION = "1";

    @SubscribeEvent
    public static void onPayloadRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        NetworkRegistrar.register(registrar, AnvilLibSpaceSelect.MOD_ID);
    }
}
