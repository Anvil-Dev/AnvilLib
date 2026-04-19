/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/util/DataIngredient.java
 */

package dev.anvilcraft.lib.v2.registrum.util;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ObjectArrays;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;

import lombok.Getter;
import lombok.experimental.Delegate;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * A helper for data generation when using ingredients as input(s) to recipes.<br>
 * It remembers the name of the primary ingredient for use in creating recipe names/criteria.
 * <p>
 * Create an instance of this class with the various factory methods such as {@link #items(ItemLike, ItemLike...)} and {@link #tag(TagKey)}.
 * <p>
 * <strong>This class should not be used for any purpose other than data generation</strong>, it will throw an exception if it is serialized to a packet buffer.
 */
public final class DataIngredient {
    private interface Excludes {

        void toNetwork(FriendlyByteBuf buffer);

        boolean checkInvalidation();

        void markValid();

        boolean isVanilla();

        ItemStack[] getItems();

        Ingredient.Value[] getValues();
    }

    @Delegate(excludes = Excludes.class)
    private final Ingredient parent;
    @Getter
    private final ResourceLocation id;
    private final Function<RegistrumRecipeProvider, Criterion<InventoryChangeTrigger.TriggerInstance>> criteriaFactory;

    private DataIngredient(Ingredient parent, ItemLike item) {
        this.parent = parent;
        this.id = BuiltInRegistries.ITEM.getKey(item.asItem());
        this.criteriaFactory = prov -> RegistrumRecipeProvider.has(item);
    }
    
    private DataIngredient(Ingredient parent, TagKey<Item> tag) {
        this.parent = parent;
        this.id = tag.location();
        this.criteriaFactory = prov -> RegistrumRecipeProvider.has(tag);
    }
    
    private DataIngredient(Ingredient parent, ResourceLocation id, ItemPredicate... predicates) {
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

    public static DataIngredient stacks(ItemStack first, ItemStack... others) {
        return ingredient(Ingredient.of(ObjectArrays.concat(first, others)), first.getItem());
    }

    public static DataIngredient tag(TagKey<Item> tag) {
        return ingredient(Ingredient.of(tag), tag);
    }
    
    public static DataIngredient ingredient(Ingredient parent, ItemLike required) {
        return new DataIngredient(parent, required);
    }
    
    public static DataIngredient ingredient(Ingredient parent, TagKey<Item> required) {
        return new DataIngredient(parent, required);
    }
    
    public static DataIngredient ingredient(Ingredient parent, ResourceLocation id, ItemPredicate... criteria) {
        return new DataIngredient(parent, id, criteria);
    }

    public Ingredient toVanilla() {
        return parent;
    }
}
