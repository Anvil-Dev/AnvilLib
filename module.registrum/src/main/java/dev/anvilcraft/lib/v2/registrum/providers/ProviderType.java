/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/providers/ProviderType.java
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumLootTableProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a type of data that can be generated, and specifies a factory for the provider.
 * <p>
 * Used as a key for data generator callbacks.
 * <p>
 * This file also defines the built-in provider types, but third-party types can be created with {@link #register(String, ProviderType)}.
 *
 * @param <T> The type of the provider
 */
@FunctionalInterface
@SuppressWarnings("deprecation")
public interface ProviderType<T extends RegistrumProvider> {

    // SERVER DATA
    ProviderType<RegistrumDatapackProvider> DYNAMIC = registerServerData("dynamic", RegistrumDatapackProvider::new);
    ProviderType<RegistrumDataMapProvider> DATA_MAP = registerServerData("data_map", RegistrumDataMapProvider::new);
    ProviderType<RegistrumRecipeProvider> RECIPE = registerServerData("recipe", RegistrumRecipeProvider::new);
    ProviderType<RegistrumAdvancementProvider> ADVANCEMENT = registerServerData("advancement", RegistrumAdvancementProvider::new);
    ProviderType<RegistrumLootTableProvider> LOOT = registerServerData("loot", RegistrumLootTableProvider::new);
    ProviderType<RegistrumTagsProvider.IntrinsicImpl<Block>> BLOCK_TAGS = registerIntrinsicTag("tags/block", "blocks", Registries.BLOCK, block -> block.builtInRegistryHolder().key());
    ProviderType<RegistrumTagsProvider.Impl<Enchantment>> ENCHANTMENT_TAGS = registerDynamicTag("tags/enchantment", "enchantments", Registries.ENCHANTMENT);
    ProviderType<RegistrumTagsProvider.Impl<DamageType>> DAMAGE_TYPE_TAGS = registerDynamicTag("tags/damage_type", "damage_types", Registries.DAMAGE_TYPE);
    ProviderType<RegistrumItemTagsProvider> ITEM_TAGS = registerTag("tags/item", Registries.ITEM, c -> new RegistrumItemTagsProvider(c.parent(), c.type(), "items", c.output(), c.provider(), c.get(BLOCK_TAGS).contentsGetter(), c.fileHelper()));
    ProviderType<RegistrumTagsProvider.IntrinsicImpl<Fluid>> FLUID_TAGS = registerIntrinsicTag("tags/fluid", "fluids", Registries.FLUID, fluid -> fluid.builtInRegistryHolder().key());
    ProviderType<RegistrumTagsProvider.IntrinsicImpl<EntityType<?>>> ENTITY_TAGS = registerIntrinsicTag("tags/entity", "entity_types", Registries.ENTITY_TYPE, entityType -> entityType.builtInRegistryHolder().key());
    ProviderType<RegistrumGenericProvider> GENERIC_SERVER = registerProvider("registrate_generic_server_provider",  c -> new RegistrumGenericProvider(c.parent(), c.event(), LogicalSide.SERVER, c.type()));

    // CLIENT DATA
    ProviderType<RegistrumBlockstateProvider> BLOCKSTATE = registerProvider("blockstate", c -> new RegistrumBlockstateProvider(c.parent(), c.output(), c.fileHelper()));
    ProviderType<RegistrumItemModelProvider> ITEM_MODEL = registerProvider("item_model", c -> new RegistrumItemModelProvider(c.parent(), c.output(), c.get(BLOCKSTATE).getExistingFileHelper()));
    ProviderType<RegistrumLangProvider> LANG = registerProvider("lang", c -> new RegistrumLangProvider(c.parent(), c.output()));
    ProviderType<RegistrumGenericProvider> GENERIC_CLIENT = registerProvider("registrate_generic_client_provider", c -> new RegistrumGenericProvider(c.parent(), c.event(), LogicalSide.CLIENT, c.type()));

    record Context<T extends RegistrumProvider>(ProviderType<T> type, AbstractRegistrum<?> parent,
                                                @Deprecated GatherDataEvent event,
                                                Map<ProviderType<?>, RegistrumProvider> existing,
                                                PackOutput output, ExistingFileHelper fileHelper,
                                                CompletableFuture<HolderLookup.Provider> provider) {

        public <R extends RegistrumProvider> R get(ProviderType<R> other) {
            return (R) existing().get(other);
        }

    }

    default T create(Context<T> context) {
        return create(context.parent(), context.event(), context.existing());
    }

    @Deprecated
    T create(AbstractRegistrum<?> parent, GatherDataEvent event, Map<ProviderType<?>, RegistrumProvider> existing);

    interface DependencyAwareProviderType<T extends RegistrumProvider> extends ProviderType<T> {

        @Override
        default T create(AbstractRegistrum<?> parent, GatherDataEvent event, Map<ProviderType<?>, RegistrumProvider> existing) {
            return create(new Context<>(this, parent, event, existing, event.getGenerator().getPackOutput(), event.getExistingFileHelper(), event.getLookupProvider()));
        }

        @Override
        T create(Context<T> context);

    }

    interface SimpleServerDataFactory<T extends RegistrumProvider> extends DependencyAwareProviderType<T> {

        T create(AbstractRegistrum<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider);

        @Override
        default T create(Context<T> context) {
            return create(context.parent(), context.output(), context.provider());
        }

        default ProviderType<T> asProvider() {
            return this;
        }

    }

    // TODO this is clunky af
    @Deprecated
    static <T extends RegistrumProvider> ProviderType<T> registerDelegate(String name, NonNullUnaryOperator<ProviderType<T>> type) {
        ProviderType<T> ret = new ProviderType<T>() {

            @Override
            public T create(AbstractRegistrum<?> parent, GatherDataEvent event, Map<ProviderType<?>, RegistrumProvider> existing) {
                return type.apply(this).create(parent, event, existing);
            }
        };
        return register(name, ret);
    }

    @Deprecated
    static <T extends RegistrumProvider> ProviderType<T> register(String name, NonNullFunction<ProviderType<T>, NonNullBiFunction<AbstractRegistrum<?>, GatherDataEvent, T>> type) {
        ProviderType<T> ret = new ProviderType<T>() {

            @Override
            public T create(AbstractRegistrum<?> parent, GatherDataEvent event, Map<ProviderType<?>, RegistrumProvider> existing) {
                return type.apply(this).apply(parent, event);
            }
        };
        return register(name, ret);
    }

    @Deprecated
    static <T extends RegistrumProvider> ProviderType<T> register(String name, NonNullBiFunction<AbstractRegistrum<?>, GatherDataEvent, T> type) {
        ProviderType<T> ret = (parent, event, existing) -> type.apply(parent, event);
        return register(name, ret);
    }

    @Deprecated
    static <T extends RegistrumProvider> ProviderType<T> register(String name, ProviderType<T> type) {
        RegistrumDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T extends RegistrumProvider> ProviderType<T> registerServerData(String name, SimpleServerDataFactory<T> factory) {
        return register(name, factory.asProvider());
    }

    static <T extends RegistrumProvider> ProviderType<T> registerProvider(String name, DependencyAwareProviderType<T> type) {
        RegistrumDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T, R extends RegistrumTagsProvider<T>> ProviderType<R> registerTag(String name, ResourceKey<? extends Registry<T>> key, DependencyAwareProviderType<R> type) {
        if (RegistrumDataProvider.TAG_TYPES.containsKey(key)) {
            return (ProviderType<R>) RegistrumDataProvider.TAG_TYPES.get(key);
        }
        RegistrumDataProvider.TAG_TYPES.put(key, type);
        RegistrumDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T> ProviderType<RegistrumTagsProvider.IntrinsicImpl<T>> registerIntrinsicTag(String providerName, String typeName, ResourceKey<? extends Registry<T>> registry, Function<T, ResourceKey<T>> keyExtractor) {
        return registerTag(providerName, registry, c -> new RegistrumTagsProvider.IntrinsicImpl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), keyExtractor, c.fileHelper()));
    }

    static <T> ProviderType<RegistrumTagsProvider.Impl<T>> registerDynamicTag(String providerName, String typeName, ResourceKey<Registry<T>> registry) {
        return registerTag(providerName, registry, c -> new RegistrumTagsProvider.Impl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), c.fileHelper()));
    }

    static <T extends RegistrumProvider> T create(ProviderType<T> type, AbstractRegistrum<?> parent, GatherDataEvent event, Map<ProviderType<?>, RegistrumProvider> existing, CompletableFuture<HolderLookup.Provider> provider) {
        return type.create(new Context<>(type, parent, event, existing, event.getGenerator().getPackOutput(), event.getExistingFileHelper(), provider));
    }

}
