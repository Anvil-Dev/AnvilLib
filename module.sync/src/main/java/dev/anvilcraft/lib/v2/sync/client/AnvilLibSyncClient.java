package dev.anvilcraft.lib.v2.sync.client;

import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.management.SyncConfigManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = AnvilLibSync.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibSyncClient {
    public static final SyncConfigManager SYNC_CONFIG_MANAGER = new SyncConfigManager();

    public AnvilLibSyncClient(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    /**
     * 每客户端 tick 结束时扫描惰性同步目标并上行变更（受字段方向限制）。
     */
    public void onClientTick(ClientTickEvent.Post event) {
        AnvilLibSync.LAZY_SYNC_MANAGER.tickClient();
    }

    /**
     * 客户端登出时清理客户端侧惰性同步跟踪状态，避免跨存档 / 跨服残留。
     */
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AnvilLibSync.LAZY_SYNC_MANAGER.clearClient();
    }
}
