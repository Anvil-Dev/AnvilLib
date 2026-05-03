/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/DataIngredient.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util;

import com.google.common.collect.ObjectArrays;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import lombok.Getter;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A helper for data generation when using ingredients as input(s) to recipes.<br>
 * It remembers the name of the primary ingredient for use in creating recipe names/criteria.
 * <p>
 * Create an instance of this class with the various factory methods such as {@link #items(ItemLike, ItemLike...)} and {@link #tag(TagKey)}.
 * <p>
 * <strong>This class should not be used for any purpose other than data generation</strong>, it will throw an exception if it is serialized to a packet buffer.
 */
public final class DataIngredient {

    //TODO <1.21.5> removed delegate. Is there a need to add it back?
    private final Ingredient parent;
    @Getter
    private final Identifier id;
    private final Function<RegistrumRecipeProvider, Criterion<InventoryChangeTrigger.TriggerInstance>> criteriaFactory;

    private DataIngredient(Ingredient parent, ItemLike item) {
        this.parent = parent;
        this.id = BuiltInRegistries.ITEM.getKey(item.asItem());
        this.criteriaFactory = prov -> prov.has(item);
    }

    private DataIngredient(Ingredient parent, TagKey<Item> tag) {
        this.parent = parent;
        this.id = tag.location();
        this.criteriaFactory = prov -> prov.has(tag);
    }

    private DataIngredient(Ingredient parent, Identifier id, ItemPredicate... predicates) {
        this.parent = parent;
        this.id = id;
        this.criteriaFactory = prov -> RegistrumRecipeProvider.inventoryTrigger(predicates);
    }

    public Criterion<InventoryChangeTrigger.TriggerInstance> getCriterion(RegistrumRecipeProvider prov) {
        return criteriaFactory.apply(prov);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T extends ItemLike> DataIngredient items(NonNullSupplier<? extends T> first, NonNullSupplier<? extends T>... others) {
        return items(first.get(), (T[]) Arrays.stream(others).map(Supplier::get).toArray(ItemLike[]::new));
    }

    @SafeVarargs
    public static <T extends ItemLike> DataIngredient items(T first, T... others) {
        return ingredient(Ingredient.of(ObjectArrays.concat(first, others)), first);
    }

    public static DataIngredient tag(TagKey<Item> tag) {
        return ingredient(Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(tag)), tag);
    }

    public static DataIngredient ingredient(Ingredient parent, ItemLike required) {
        return new DataIngredient(parent, required);
    }

    public static DataIngredient ingredient(Ingredient parent, TagKey<Item> required) {
        return new DataIngredient(parent, required);
    }

    public static DataIngredient ingredient(Ingredient parent, Identifier id, ItemPredicate... criteria) {
        return new DataIngredient(parent, id, criteria);
    }

    public Ingredient toVanilla() {
        return parent;
    }
}
