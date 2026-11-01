/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/DataProviderInitializer.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.core.registries.SingleRegistryBootstrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public class DataProviderInitializer {

    private final RegistrySetBuilder datapackEntryProvider = new RegistrySetBuilder();
    private final RegistrySetBuilder reloadableDatapackEntryProvider = new RegistrySetBuilder();
    private final List<Runnable> pendingRegistryProviders = new ArrayList<>();

    private final Map<ProviderType<?>, ProviderType<? extends RegistrumLookupFillerProvider>> providerDependencies = new ConcurrentHashMap<>();

    public DataProviderInitializer() {
        addDependency(ProviderType.ITEM_TAGS, ProviderType.BLOCK_TAGS);
        addDependency(ProviderType.RELOADABLE_DYNAMIC, ProviderType.DYNAMIC);
        addDependency(ProviderType.RECIPE_RUNNER, ProviderType.RELOADABLE_DYNAMIC);
        addDependency(ProviderType.LOOT, ProviderType.RELOADABLE_DYNAMIC);
        addDependency(ProviderType.ADVANCEMENT, ProviderType.RELOADABLE_DYNAMIC);
    }

    protected RegistrySetBuilder getDatapackRegistryProviders() {
        prepareRegistryProviders();
        return datapackEntryProvider;
    }

    protected RegistrySetBuilder getReloadableDatapackRegistryProviders() {
        prepareRegistryProviders();
        return reloadableDatapackEntryProvider;
    }

    private void prepareRegistryProviders() {
        this.pendingRegistryProviders.forEach(Runnable::run);
        this.pendingRegistryProviders.clear();
    }

    protected List<Sorted> getSortedProviders() {
        List<Sorted> ans = new ArrayList<>();
        Set<ProviderType<?>> added = new HashSet<>();
        List<Map.Entry<String, ProviderType<?>>> remain = new ArrayList<>(RegistrumDataProvider.TYPES.entrySet());
        while (!remain.isEmpty()) {
            if (!remain.removeIf(e -> {
                ProviderType<?> type = e.getValue();
                var parent = providerDependencies.get(type);
                if (parent == null || added.contains(parent)) {
                    ans.add(new Sorted(e.getKey(), type, parent));
                    added.add(type);
                    return true;
                }
                return false;
            })) {
                throw new IllegalStateException("Looping dependency detected: " + remain);
            }
        }
        return ans;
    }

    public <T> void add(ResourceKey<Registry<T>> registry, SingleRegistryBootstrap<T> provider) {
        // 模组构造时原生注册表尚未初始化，延迟到 GatherDataEvent 再确定所属层。
        this.pendingRegistryProviders.add(() -> {
            if (RegistryDataLoader.RELOADABLE_REGISTRIES.stream().anyMatch(data -> data.key().equals(registry))) {
                reloadableDatapackEntryProvider.add(registry, provider);
            } else {
                datapackEntryProvider.add(registry, provider);
            }
        });
    }

    public void addDependency(ProviderType<?> dependent, ProviderType<? extends RegistrumLookupFillerProvider> parent) {
        var old = providerDependencies.put(dependent, parent);
        if (old != null) throw new IllegalStateException("Providers can have only 1 prerequisite");
    }

    public record Sorted(
        String id, ProviderType<?> type,
        @Nullable ProviderType<? extends RegistrumLookupFillerProvider> parent
    ) {
    }

}
