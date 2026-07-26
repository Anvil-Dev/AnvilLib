/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateDataProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.DebugMarkers;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

@Log4j2
@ApiStatus.Internal
public class RegistrumDataProvider implements DataProvider {

    @SuppressWarnings("null")
    static final BiMap<String, ProviderType<?>> TYPES = HashBiMap.create();

    static final Map<ResourceKey<? extends Registry<?>>, ProviderType<?>> TAG_TYPES = new ConcurrentHashMap<>();

    public static @Nullable String getTypeName(GeneratorType<?> type) {
        if (type instanceof ProviderType<?> prov) {
            return TYPES.inverse().get(prov);
        }
        return type.toString();
    }

    private final String mod;
    private final Map<ProviderType<?>, RegistrumProvider> subProviders = new LinkedHashMap<>();
    private final Map<GeneratorType<?>, Object> subGenerators = new LinkedHashMap<>();
    private final CompletableFuture<HolderLookup.Provider> registriesLookup;

    public RegistrumDataProvider(AbstractRegistrum<?> parent, String modid, GatherDataEvent event) {
        this.mod = modid;
        this.registriesLookup = event.getLookupProvider();

        // For now, generate everything together
        /*
        EnumSet<LogicalSide> sides = EnumSet.noneOf(LogicalSide.class);
        if (event.includeServer()) {
            sides.add(LogicalSide.SERVER);
        }
        if (event.includeClient()) {
            sides.add(LogicalSide.CLIENT);
        }
        */

        //log.debug(DebugMarkers.DATA, "Gathering providers for sides: {}", sides);
        log.debug(DebugMarkers.DATA, "Gathering providers");
        Map<ProviderType<?>, RegistrumProvider> known = new HashMap<>();
        for (DataProviderInitializer.Sorted sorted : parent.getDataGenInitializer().getSortedProviders()) {
            ProviderType<?> type = sorted.type();
            var lookup = registriesLookup;
            if (sorted.parent() != null) lookup = ((RegistrumLookupFillerProvider) known.get(sorted.parent())).getFilledProvider();
            RegistrumProvider prov = ProviderType.create(type, parent, event, known, lookup);
            if (prov instanceof RegistrumTagsProvider<?> tagsProvider && TAG_TYPES.get(tagsProvider.registry()) != type) {
                throw new IllegalStateException("Tag providers must be registered through ProviderType::registerTag");
            }
            known.put(type, prov);
            // if (sides.contains(prov.getSide())) {
            log.debug(DebugMarkers.DATA, "Adding provider for type: {}", sorted.id());
            subProviders.put(type, prov);
            //}
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registriesLookup.thenCompose(_ -> {
            var list = Lists.<CompletableFuture<?>>newArrayList();

            for (Map.Entry<@NonnullType ProviderType<?>, RegistrumProvider> e : subProviders.entrySet()) {
                log.debug(DebugMarkers.DATA, "Generating data for type: {}", getTypeName(e.getKey()));
                list.add(e.getValue().run(cache));
            }

            return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Registrum Provider for " + mod + " [" + subProviders.values()
            .stream()
            .map(DataProvider::getName)
            .collect(Collectors.joining(", ")) + "]";
    }

    @SuppressWarnings("unchecked")
    public <P> Optional<P> getSubProvider(GeneratorType<P> type) {
        if (type instanceof ProviderType<?> prov) {
            return Optional.ofNullable((P) subProviders.get(prov));
        }
        return Optional.ofNullable((P) subGenerators.get(type));
    }

    public <T> void putSubProvider(GeneratorType<? extends T> type, T gen) {
        subGenerators.put(type, gen);
    }

}
