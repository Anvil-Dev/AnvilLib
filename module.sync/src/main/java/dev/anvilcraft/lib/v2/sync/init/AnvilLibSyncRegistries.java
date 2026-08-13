package dev.anvilcraft.lib.v2.sync.init;

import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.management.SyncRegisterEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibSync.MOD_ID)
public class AnvilLibSyncRegistries {
    public static final ResourceKey<Registry<SyncRegisterEntry<?, ?>>> SYNC_ENTRY = ResourceKey.createRegistryKey(
        AnvilLibSync.of("sync_entry")
    );
    public static final Registry<SyncRegisterEntry<?, ?>> SYNC_ENTRY_REGISTRY = new RegistryBuilder<>(SYNC_ENTRY)
        .maxId(512)
        .create();

    @ApiStatus.Internal
    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(SYNC_ENTRY_REGISTRY);
    }
}
