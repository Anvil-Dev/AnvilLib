/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/builders/EntityBuilder.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumEntityLootTables;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumLootTableProvider.LootType;
import dev.anvilcraft.lib.v2.registrum.util.OneTimeEventReceiver;
import dev.anvilcraft.lib.v2.registrum.util.RegistrumDistExecutor;
import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements.SpawnPredicate;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * A builder for entities, allows for customization of the {@link EntityType.Builder}, easy creation of spawn egg items, and configuration of data associated with entities (loot tables, etc.).
 *
 * @param <T> The type of entity being built
 * @param <P> Parent object type
 */
public class EntityBuilder<T extends Entity, P> extends AbstractBuilder<EntityType<?>, EntityType<T>, P, EntityBuilder<T, P>> {

    /**
     * Create a new {@link EntityBuilder} and configure data. Used in lieu of adding side effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The entity will be assigned the following data:
     * <ul>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * </ul>
     *
     * @param <T>            The type of the builder
     * @param <P>            Parent object type
     * @param owner          The owning {@link AbstractRegistrum} object
     * @param parent         The parent object
     * @param name           Name of the entry being built
     * @param callback       A callback used to actually register the built entry
     * @param factory        Factory to create the entity
     * @param classification The {@link MobCategory} of the entity
     * @return A new {@link EntityBuilder} with reasonable default data generators.
     */
    public static <T extends Entity, P> EntityBuilder<T, P> create(
        AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, EntityType.EntityFactory<T> factory,
        MobCategory classification
    ) {
        return new EntityBuilder<>(owner, parent, name, callback, factory, classification)
            .defaultLang();
    }

    private final NonNullSupplier<EntityType.Builder<T>> builder;

    private NonNullConsumer<EntityType.Builder<T>> builderCallback = _ -> {
    };

    @Nullable
    private NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T, ?>>> renderer;

    private boolean attributesConfigured, spawnConfigured; // TODO make this more reuse friendly

    protected EntityBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        EntityType.EntityFactory<T> factory,
        MobCategory classification
    ) {
        super(owner, parent, name, callback, Registries.ENTITY_TYPE);
        this.builder = () -> EntityType.Builder.of(factory, classification);
    }

    /**
     * Modify the properties of the entity. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     *
     * @param cons The action to perform on the properties
     * @return this {@link EntityBuilder}
     */
    public EntityBuilder<T, P> properties(NonNullConsumer<EntityType.Builder<T>> cons) {
        builderCallback = builderCallback.andThen(cons);
        return this;
    }

    /**
     * Register an {@link EntityRenderer} for this entity.
     * <p>
     *
     * @param renderer A (server safe) supplier to an {@link EntityRendererProvider} that will provide this entity's renderer
     * @return this {@link EntityBuilder}
     */
    public EntityBuilder<T, P> renderer(NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T, ?>>> renderer) {
        if (this.renderer == null && FMLLoader.getCurrent().getDist().isClient()) { // First call only
            RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerRenderer);
        }
        this.renderer = renderer;
        return this;
    }

    protected void registerRenderer() {
        OneTimeEventReceiver.addModListener(
            getOwner(), EntityRenderersEvent.RegisterRenderers.class, evt -> {
                var renderer = this.renderer;
                if (renderer != null) {
                    try {
                        var provider = renderer.get();
                        evt.registerEntityRenderer(getEntry(), provider::apply);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to register renderer for Entity " + get().getId(), e);
                    }
                }
            }
        );
    }

    /**
     * Register an attributes for this entity. The entity must extend {@link LivingEntity}.
     * <p>
     * Cannot be called more than once per builder.
     *
     * @param attributes A supplier to the attributes for this entity, usually of the form {@code EntityClass::createAttributes}
     * @return this {@link EntityBuilder}
     * @throws IllegalStateException When called more than once
     */
    @SuppressWarnings("unchecked")
    public EntityBuilder<T, P> attributes(Supplier<AttributeSupplier.Builder> attributes) {
        if (attributesConfigured) {
            throw new IllegalStateException("Cannot configure attributes more than once");
        }
        attributesConfigured = true;
        OneTimeEventReceiver.addModListener(
            getOwner(),
            EntityAttributeCreationEvent.class,
            e -> e.put((EntityType<LivingEntity>) getEntry(), attributes.get().build())
        );
        return this;
    }

    /**
     * Register a spawn placement for this entity. The entity must extend {@link Mob} and allow construction with a {@code null} {@link Level}.
     * <p>
     * Cannot be called more than once per builder.
     *
     * @param type      The type of placement to use
     * @param heightmap Which heightmap to use to choose placement locations
     * @param predicate A predicate to check spawn locations for validity
     * @return this {@link EntityBuilder}
     * @throws IllegalStateException When called more than once
     */
    @SuppressWarnings("unchecked")
    public EntityBuilder<T, P> spawnPlacement(
        SpawnPlacementType type,
        Heightmap.Types heightmap,
        SpawnPredicate<T> predicate,
        RegisterSpawnPlacementsEvent.Operation operation
    ) {
        if (spawnConfigured) {
            throw new IllegalStateException("Cannot configure spawn placement more than once");
        }
        spawnConfigured = true;
        this.onRegister(t -> {
            /* TODO is there any way to do this now?
            try {
                if (!(t.create(null) instanceof MobEntity)) {
                    throw new IllegalArgumentException("Cannot register spawn placement for entity " + t.getRegistryName() + " as it does not extend MobEntity");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to type check entity " + t.getRegistryName() + " when registering spawn placement", e);
            }
            */
            OneTimeEventReceiver.addModListener(
                getOwner(), RegisterSpawnPlacementsEvent.class, e -> {
                    e.register(t, type, heightmap, predicate, operation);
                }
            );
        });
        return this;
    }

    /**
     * Assign the default translation, as specified by {@link RegistrumLangProvider#getAutomaticName(NonNullSupplier, net.minecraft.resources.ResourceKey)}. This is the default, so it is generally
     * not necessary to call, unless for undoing previous changes.
     *
     * @return this {@link EntityBuilder}
     */
    public EntityBuilder<T, P> defaultLang() {
        return lang(EntityType::getDescriptionId);
    }

    /**
     * Set the translation for this entity.
     *
     * @param name A localized English name
     * @return this {@link EntityBuilder}
     */
    public EntityBuilder<T, P> lang(String name) {
        return lang(EntityType::getDescriptionId, name);
    }

    /**
     * Configure the loot table for this entity. This is different from most data gen callbacks as the callback does not accept a {@link DataGenContext}, but instead a
     * {@link RegistrumEntityLootTables}, for creating specifically entity loot tables.
     *
     * @param cons The callback which will be invoked during entity loot table creation.
     * @return this {@link EntityBuilder}
     */
    public EntityBuilder<T, P> loot(NonNullBiConsumer<RegistrumEntityLootTables, EntityType<T>> cons) {
        return setData(ProviderType.LOOT, (ctx, prov) -> prov.addLootAction(LootType.ENTITY, tb -> cons.accept(tb, ctx.getEntry())));
    }

    /**
     * Assign {@link TagKey}{@code s} to this entity. Multiple calls will add additional tags.
     *
     * @param tags The tags to assign
     * @return this {@link EntityBuilder}
     */
    @SafeVarargs
    public final EntityBuilder<T, P> tag(TagKey<EntityType<?>>... tags) {
        return tag(ProviderType.ENTITY_TAGS, tags);
    }

    @Override
    protected EntityType<T> createEntry() {
        EntityType.Builder<T> builder = this.builder.get();
        builderCallback.accept(builder);
        return builder.build(getResourceKey());
    }

    @Deprecated
    protected void injectSpawnEggType(EntityType<T> entry) {
    }

    @Override
    protected RegistryEntry<EntityType<?>, EntityType<T>> createEntryWrapper(DeferredHolder<EntityType<?>, EntityType<T>> delegate) {
        return new EntityEntry<>(getOwner(), delegate);
    }

    @Override
    public EntityEntry<T> register() {
        return (EntityEntry<T>) super.register();
    }
}
