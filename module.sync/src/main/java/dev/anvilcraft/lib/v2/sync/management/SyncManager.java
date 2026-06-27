package dev.anvilcraft.lib.v2.sync.management;

import dev.anvilcraft.lib.v2.sync.init.AnvilLibSyncRegistries;
import dev.anvilcraft.lib.v2.sync.network.payload.SyncPayload;
import dev.anvilcraft.lib.v2.sync.util.SideUtil;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class SyncManager {
    private final Map<Class<?>, SyncRegisterEntry<?, ?>> syncRegisterEntryMap = new HashMap<>();

    @ApiStatus.Internal
    public void compileContent() {
        Set<Map.Entry<ResourceKey<SyncRegisterEntry<?, ?>>, SyncRegisterEntry<?, ?>>> syncEntries = AnvilLibSyncRegistries.SYNC_ENTRY_REGISTRY.entrySet();
        log.info("SYNC_ENTRY_REGISTRY Size: {}", syncEntries.size());
        syncEntries.forEach(entry -> this.syncRegisterEntryMap.put(entry.getValue().clazz(), entry.getValue()));
    }

    <T, P, ID> void setValue(@Nullable P parent, SyncProxy<T> proxy, T oldValue, T newValue) {
        Objects.requireNonNull(parent, "Parent cannot be null when setting value");
        SyncRegisterEntry<P, ID> entry = checkParent(parent);
        ID id = entry.idGetter().apply(parent);
        SideUtil.send(
            proxy.getDirection(),
            entry.dimension(),
            entry.dimensionGetter(),
            parent,
            id,
            (flow) -> SyncPayload.create(parent, entry.idCodec(), id, proxy, flow)
        );
    }

    <T> void getValue(@Nullable Object parent, SyncProxy<T> proxy, T value) {
    }

    <P, ID> SyncRegisterEntry<P, ID> checkParent(Object parent) {
        return Objects.requireNonNull(
            this.contains(parent.getClass()),
            "Parent class " + parent.getClass().getName() + " is not registered for syncing"
        );
    }

    @Nullable
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public <P, ID> SyncRegisterEntry<P, ID> contains(Class<?> clazz) {
        boolean containsKey = syncRegisterEntryMap.containsKey(clazz);
        if (containsKey) return (SyncRegisterEntry<P, ID>) syncRegisterEntryMap.get(clazz);
        Set<Class<?>> classes = syncRegisterEntryMap.keySet();
        for (Class<?> classKey : classes) {
            if (classKey.isAssignableFrom(clazz)) {
                return (SyncRegisterEntry<P, ID>) syncRegisterEntryMap.get(classKey);
            }
        }
        return null;
    }
}
