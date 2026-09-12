/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/ProviderType.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumModelProvider;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeRunner;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumLootTableProvider;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
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
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a type of data that can be generated, and specifies a factory for the provider.
 * <p>
 * Used as a key for data generator callbacks.
 * <p>
 * This file also defines the built-in provider types, but third-party types can be created with {@link #registerProvider(String, ProviderType)}.
 *
 * @param <T> The type of the provider
 */
@FunctionalInterface
@SuppressWarnings(
    {
        "deprecation",
        "unused"
    }
)
public interface ProviderType<T extends RegistrumProvider> extends GeneratorType<T> {

    // SERVER DATA
    ProviderType<RegistrumDatapackProvider> DYNAMIC = registerServerData("dynamic", RegistrumDatapackProvider::new);
    ProviderType<RegistrumDataMapProvider> DATA_MAP = registerServerData("data_map", RegistrumDataMapProvider::new);
    ProviderType<RegistrumRecipeRunner> RECIPE_RUNNER = registerServerData("recipe_runner", RegistrumRecipeRunner::new);
    ProviderType<RegistrumAdvancementProvider> ADVANCEMENT = registerServerData("advancement", RegistrumAdvancementProvider::new);
    ProviderType<RegistrumLootTableProvider> LOOT = registerServerData("loot", RegistrumLootTableProvider::new);
    ProviderType<RegistrumTagsProvider.IntrinsicImpl<Block>> BLOCK_TAGS = registerIntrinsicTag(
        "tags/block",
        "blocks",
        Registries.BLOCK,
        block -> block.builtInRegistryHolder().key()
    );
    ProviderType<RegistrumTagsProvider.Impl<Enchantment>> ENCHANTMENT_TAGS = registerDynamicTag(
        "tags/enchantment",
        "enchantments",
        Registries.ENCHANTMENT
    );
    ProviderType<RegistrumTagsProvider.Impl<DamageType>> DAMAGE_TYPE_TAGS = registerDynamicTag(
        "tags/damage_type",
        "damage_types",
        Registries.DAMAGE_TYPE
    );
    ProviderType<RegistrumItemTagsProvider> ITEM_TAGS = registerTag(
        "tags/item",
        Registries.ITEM,
        c -> new RegistrumItemTagsProvider(c.parent(), c.type(), "items", c.output(), c.provider(), c.get(BLOCK_TAGS).contentsGetter())
    );
    ProviderType<RegistrumTagsProvider.IntrinsicImpl<Fluid>> FLUID_TAGS = registerIntrinsicTag(
        "tags/fluid",
        "fluids",
        Registries.FLUID,
        fluid -> fluid.builtInRegistryHolder().key()
    );
    ProviderType<RegistrumTagsProvider.IntrinsicImpl<EntityType<?>>> ENTITY_TAGS = registerIntrinsicTag(
        "tags/entity",
        "entity_types",
        Registries.ENTITY_TYPE,
        entityType -> entityType.builtInRegistryHolder().key()
    );
    ProviderType<RegistrumGenericProvider> GENERIC_SERVER = registerProvider(
        "registrum_generic_server_provider",
        c -> new RegistrumGenericProvider(c.parent(), c.event(), LogicalSide.SERVER, c.type())
    );

    // CLIENT DATA
    ProviderType<RegistrumModelProvider> MODEL = registerClientProvider(
        "model",
        () -> c -> new RegistrumModelProvider(c.parent(), c.output())
    );
    ProviderType<RegistrumLangProvider> LANG = registerClientProvider("lang", () -> c -> new RegistrumLangProvider(c.parent(), c.output()));
    ProviderType<RegistrumGenericProvider> GENERIC_CLIENT = registerClientProvider(
        "registrum_generic_client_provider",
        () -> c -> new RegistrumGenericProvider(c.parent(), c.event(), LogicalSide.CLIENT, c.type())
    );

    GeneratorType<RegistrumRecipeProvider> RECIPE = RECIPE_RUNNER.createGenerator("recipe");
    GeneratorType<RegistrumBlockModelGenerator> BLOCKSTATE = MODEL.createGenerator("blockstate");
    GeneratorType<RegistrumItemModelGenerator> ITEM_MODEL = MODEL.createGenerator("item_model");

    record Context<T extends RegistrumProvider>(
        ProviderType<T> type, AbstractRegistrum<?> parent,
        @Deprecated GatherDataEvent event,
        Map<ProviderType<?>, RegistrumProvider> existing,
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> provider
    ) {

        public <R extends RegistrumProvider> R get(ProviderType<R> other) {
            return (R) existing().get(other);
        }

    }

    T create(Context<T> context);

    default <R> GeneratorType<R> createGenerator(String type) {
        return new GeneratorType<>() {
            public String toString() {
                return type;
            }
        };
    }

    interface SimpleServerDataFactory<T extends RegistrumProvider> extends ProviderType<T> {

        T create(AbstractRegistrum<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider);

        @Override
        default T create(Context<T> context) {
            return create(context.parent(), context.output(), context.provider());
        }

        default ProviderType<T> asProvider() {
            return this;
        }

    }

    static <T extends RegistrumProvider> ProviderType<T> registerServerData(String name, SimpleServerDataFactory<T> factory) {
        return registerProvider(name, factory.asProvider());
    }

    static <T extends RegistrumProvider> ProviderType<T> registerProvider(String name, ProviderType<T> type) {
        RegistrumDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T extends RegistrumProvider> ProviderType<T> registerClientProvider(String name, NonNullSupplier<ProviderType<T>> supplier) {
        if (!DatagenModLoader.isRunningDataGen()) return context -> null;
        var type = supplier.get();
        RegistrumDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T, R extends RegistrumTagsProvider<T>> ProviderType<R> registerTag(
        String name,
        ResourceKey<? extends Registry<T>> key,
        ProviderType<R> type
    ) {
        if (RegistrumDataProvider.TAG_TYPES.containsKey(key)) {
            return (ProviderType<R>) RegistrumDataProvider.TAG_TYPES.get(key);
        }
        RegistrumDataProvider.TAG_TYPES.put(key, type);
        RegistrumDataProvider.TYPES.put(name, type);
        return type;
    }

    static <T> ProviderType<RegistrumTagsProvider.IntrinsicImpl<T>> registerIntrinsicTag(
        String providerName,
        String typeName,
        ResourceKey<? extends Registry<T>> registry,
        Function<T, ResourceKey<T>> keyExtractor
    ) {
        return registerTag(
            providerName,
            registry,
            c -> new RegistrumTagsProvider.IntrinsicImpl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), keyExtractor)
        );
    }

    static <T> ProviderType<RegistrumTagsProvider.Impl<T>> registerDynamicTag(
        String providerName,
        String typeName,
        ResourceKey<Registry<T>> registry
    ) {
        return registerTag(
            providerName,
            registry,
            c -> new RegistrumTagsProvider.Impl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider())
        );
    }

    static <T extends RegistrumProvider> T create(
        ProviderType<T> type,
        AbstractRegistrum<?> parent,
        GatherDataEvent event,
        Map<ProviderType<?>, RegistrumProvider> existing,
        CompletableFuture<HolderLookup.Provider> provider
    ) {
        return type.create(new Context<>(type, parent, event, existing, event.getGenerator().getPackOutput(), provider));
    }

}
