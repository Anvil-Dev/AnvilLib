/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/loot/RegistrateEntityLootTables.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.loot;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RegistrumEntityLootTables extends EntityLootSubProvider implements RegistrumLootTables {

    private final AbstractRegistrum<?> parent;
    private final Consumer<RegistrumEntityLootTables> callback;

    public RegistrumEntityLootTables(
        HolderLookup.Provider provider,
        AbstractRegistrum<?> parent,
        Consumer<RegistrumEntityLootTables> callback
    ) {
        super(FeatureFlags.REGISTRY.allFlags(), provider);
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    public void generate() {
        callback.accept(this);
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return parent.getAll(Registries.ENTITY_TYPE).stream().map(Supplier::get);
    }

    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }


    @Override
    public LootItemCondition.Builder killedByFrog(HolderGetter<EntityType<?>> entityTypes) {
        return super.killedByFrog(entityTypes);
    }

    @Override
    public void add(EntityType<?> entityType, LootTable.Builder builder) {
        super.add(entityType, builder);
    }

    @Override
    public void add(EntityType<?> entityType, ResourceKey<LootTable> lootTable, LootTable.Builder builder) {
        super.add(entityType, lootTable, builder);
    }
}
