package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.rpc.config.RpcConfigurationTask;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;

@Mod(AnvilLibRpc.MOD_ID)
public class AnvilLibRpc {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_rpc";

    /**
     * 服务端 / 通用侧的权威索引表：本地扫描填充，从不被服务端下发覆盖。
     */
    public static final RpcRegistry REGISTRY = new RpcRegistry(true);

    public AnvilLibRpc(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this);
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibRpc.MAIN_ID, path);
    }

    @SubscribeEvent
    public void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new RpcConfigurationTask());
    }
}