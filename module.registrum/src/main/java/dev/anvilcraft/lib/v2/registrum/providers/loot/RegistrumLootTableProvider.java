/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/loot/RegistrateLootTableProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.loot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class RegistrumLootTableProvider extends LootTableProvider implements RegistrumProvider {

    public interface LootType<T extends RegistrumLootTables> {

        LootType<RegistrumBlockLootTables> BLOCK = register("block", LootContextParamSets.BLOCK, RegistrumBlockLootTables::new);
        LootType<RegistrumEntityLootTables> ENTITY = register("entity", LootContextParamSets.ENTITY, RegistrumEntityLootTables::new);

        T getLootCreator(HolderLookup.Provider provider, AbstractRegistrum<?> parent, Consumer<T> callback);

        ContextKeySet getLootSet();

        static <T extends RegistrumLootTables> LootType<T> register(
            String name,
            ContextKeySet set,
            TriFunction<HolderLookup.Provider, AbstractRegistrum<?>, Consumer<T>, T> factory
        ) {
            LootType<T> type = new LootType<>() {
                @Override
                public T getLootCreator(HolderLookup.Provider provider, AbstractRegistrum<?> parent, Consumer<T> callback) {
                    return factory.apply(provider, parent, callback);
                }

                @Override
                public ContextKeySet getLootSet() {
                    return set;
                }
            };
            LOOT_TYPES.put(name, type);
            return type;
        }
    }

    private static final Map<String, LootType<?>> LOOT_TYPES = new HashMap<>();

    private final AbstractRegistrum<?> parent;

    private final Multimap<LootType<?>, Consumer<? super RegistrumLootTables>> specialLootActions = HashMultimap.create();
    private final Multimap<ContextKeySet, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>>> lootActions = HashMultimap.create();
    private final Set<RegistrumLootTables> currentLootCreators = new HashSet<>();

    private final CompletableFuture<HolderLookup.Provider> provider;

    public RegistrumLootTableProvider(
        AbstractRegistrum<?> parent,
        PackOutput packOutput,
        CompletableFuture<HolderLookup.Provider> provider
    ) {
        super(packOutput, Set.of(), VanillaLootTableProvider.create(packOutput, provider).getTables(), provider);
        this.parent = parent;
        this.provider = provider;
    }

    public HolderLookup.Provider getProvider() {
        //noinspection DataFlowIssue
        return provider.getNow(null);
    }

    public <T> Holder<T> resolve(ResourceKey<T> key) {
        return getProvider().lookupOrThrow(key.registryKey()).getOrThrow(key);
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    @Override
    protected void validate(
        WritableRegistry<LootTable> tables,
        ValidationContextSource validationContext,
        ProblemReporter.Collector problems
    ) {
        currentLootCreators.forEach(c -> c.validate(tables, validationContext));
    }

    @SuppressWarnings("unchecked")
    public <T extends RegistrumLootTables> void addLootAction(LootType<T> type, NonNullConsumer<T> action) {
        this.specialLootActions.put(type, (Consumer<RegistrumLootTables>) action);
    }

    public void addLootAction(ContextKeySet set, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>> action) {
        this.lootActions.put(set, action);
    }

    private LootTableSubProvider getLootCreator(HolderLookup.Provider provider, AbstractRegistrum<?> parent, LootType<?> type) {
        RegistrumLootTables creator = type.getLootCreator(
            provider,
            parent,
            cons -> specialLootActions.get(type).forEach(c -> c.accept(cons))
        );
        currentLootCreators.add(creator);
        return creator;
    }

    @SuppressWarnings("DataFlowIssue")
    private static final @Nullable BiMap<Identifier, ContextKeySet> SET_REGISTRY = ObfuscationReflectionHelper.getPrivateValue(
        LootContextParamSets.class,
        null,
        "REGISTRY"
    );

    @Override
    public List<LootTableProvider.SubProviderEntry> getTables() {
        parent.genData(ProviderType.LOOT, this);
        currentLootCreators.clear();
        ImmutableList.Builder<LootTableProvider.SubProviderEntry> builder = ImmutableList.builder();
        for (LootType<?> type : LOOT_TYPES.values()) {
            builder.add(new SubProviderEntry(provider -> getLootCreator(provider, parent, type), type.getLootSet()));
        }
        for (ContextKeySet set : Objects.requireNonNull(SET_REGISTRY).values()) {
            builder.add(new SubProviderEntry((provider) -> callback -> lootActions.get(set).forEach(a -> a.accept(callback)), set));
        }
        return builder.build();
    }
}
