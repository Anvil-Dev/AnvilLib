package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.rpc.config.RpcConfigurationTask;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import org.jetbrains.annotations.ApiStatus;

@Mod(AnvilLibRpc.MOD_ID)
@ApiStatus.Internal
public class AnvilLibRpc {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_rpc";

    /**
     * 服务端 / 通用侧的权威索引表：本地扫描填充，从不被服务端下发覆盖。
     */
    public static final RpcRegistry REGISTRY = new RpcRegistry(true);

    /**
     * 服务端侧待响应的 {@link RPC#invoke} 调用登记表。
     */
    public static final RpcPendingCalls PENDING = new RpcPendingCalls();

    public AnvilLibRpc(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onRegisterConfigurationTasks);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibRpc.MAIN_ID, path);
    }

    public static Identifier mod(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibRpc.MOD_ID, path);
    }

    @SubscribeEvent
    public void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new RpcConfigurationTask());
    }

    /**
     * 驱动服务端侧 {@link RPC#invoke} 调用的超时检查。
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        AnvilLibRpc.PENDING.tick();
    }

    /**
     * 服务器停止时清理服务端侧未完成的 {@link RPC#invoke} 调用。
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        AnvilLibRpc.PENDING.clear();
    }
}
