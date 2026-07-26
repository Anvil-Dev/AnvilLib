package dev.anvilcraft.lib.v2.sync;

import dev.anvilcraft.lib.v2.sync.init.AnvilLibSyncEntries;
import dev.anvilcraft.lib.v2.sync.init.AnvilLibSyncRegistries;
import dev.anvilcraft.lib.v2.sync.management.LazySyncManager;
import dev.anvilcraft.lib.v2.sync.management.SyncConfigManager;
import dev.anvilcraft.lib.v2.sync.management.SyncManager;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Mod(AnvilLibSync.MOD_ID)
@ApiStatus.Internal
public class AnvilLibSync {
    public static final String MOD_ID = "anvillib_sync";
    public static final SyncManager SYNC_MANAGER = new SyncManager();
    public static final SyncConfigManager SYNC_CONFIG_MANAGER = new SyncConfigManager();
    public static final LazySyncManager LAZY_SYNC_MANAGER = new LazySyncManager();

    public AnvilLibSync(IEventBus modEventBus, ModContainer modContainer) {
        AnvilLibSyncEntries.SYNC_ENTRY.register(modEventBus);
        modEventBus.addListener(EventPriority.LOWEST, this::onRegister);
        modEventBus.addListener(this::onFMLCommonSetup);
        modEventBus.addListener(this::onRegisterConfigurationTasks);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
    }

    public void onFMLCommonSetup(FMLCommonSetupEvent event) {
        SyncConfigManager.compileContent();
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * 每服务端 tick 结束时扫描惰性同步目标并下发变更。
     */
    public void onServerTick(ServerTickEvent.Post event) {
        AnvilLibSync.LAZY_SYNC_MANAGER.tickServer();
    }

    /**
     * 服务器停止时清理服务端侧惰性同步跟踪状态。
     */
    public void onServerStopped(ServerStoppedEvent event) {
        AnvilLibSync.LAZY_SYNC_MANAGER.clearServer();
    }

    public void onRegister(RegisterEvent event) {
        if (!Objects.equals(event.getRegistryKey(), AnvilLibSyncRegistries.SYNC_ENTRY)) return;
        AnvilLibSync.SYNC_MANAGER.compileContent();
    }

    public void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncConfig(event.getListener()));
    }

    public record SyncConfig(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        public static Type TYPE = new Type(AnvilLibSync.of("sync_config"));

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            sender.accept(AnvilLibSync.SYNC_CONFIG_MANAGER.createPyload());
        }

        @Override
        public Type type() {
            return SyncConfig.TYPE;
        }
    }
}
