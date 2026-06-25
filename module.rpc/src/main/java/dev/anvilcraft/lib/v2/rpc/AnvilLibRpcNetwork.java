package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 注册 RPC 网络包。
 */
@EventBusSubscriber(modid = AnvilLibRpc.MOD_ID)
public class AnvilLibRpcNetwork {
    public static final String NETWORK_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        NetworkRegistrar.register(registrar, AnvilLibRpc.MOD_ID);
    }
}
