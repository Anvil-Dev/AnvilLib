/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/builders/BlockBuilder.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BlockEntityBuilder.BlockEntityFactory;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.GeneratorType;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumLootTableProvider.LootType;
import dev.anvilcraft.lib.v2.registrum.util.OneTimeEventReceiver;
import dev.anvilcraft.lib.v2.registrum.util.RegistrumDistExecutor;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import dev.anvilcraft.lib.v2.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A builder for blocks, allows for customization of the {@link Block.Properties}, creation of block items, and configuration of data associated with blocks (loot tables, recipes, etc.).
 *
 * @param <T> The type of block being built
 * @param <P> Parent object type
 */
@SuppressWarnings("unused")
public class BlockBuilder<T extends Block, P> extends AbstractBuilder<Block, T, P, BlockBuilder<T, P>> {

    /**
     * Create a new {@link BlockBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The block will be assigned the following data:
     * <ul>
     * <li>A default blockstate file mapping all states to one model (via {@link #defaultBlockstate()})</li>
     * <li>A simple cube_all model (used in the blockstate) with one texture (via {@link #defaultBlockstate()})</li>
     * <li>A self-dropping loot table (via {@link #defaultLoot()})</li>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * </ul>
     *
     * @param <T>      The type of the builder
     * @param <P>      Parent object type
     * @param owner    The owning {@link AbstractRegistrum} object
     * @param parent   The parent object
     * @param name     Name of the entry being built
     * @param callback A callback used to actually register the built entry
     * @param factory  Factory to create the block
     * @return A new {@link BlockBuilder} with reasonable default data generators.
     */
    public static <T extends Block, P> BlockBuilder<T, P> create(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        NonNullFunction<BlockBehaviour.Properties, T> factory
    ) {
        return new BlockBuilder<>(owner, parent, name, callback, factory, BlockBehaviour.Properties::of).defaultBlockstate()
            .defaultLoot()
            .defaultLang();
    }

    private final NonNullFunction<BlockBehaviour.Properties, T> factory;

    private NonNullSupplier<BlockBehaviour.Properties> initialProperties;
    private NonNullFunction<BlockBehaviour.Properties, BlockBehaviour.Properties> propertiesCallback = NonNullUnaryOperator.identity();

    @Nullable
    private NonNullSupplier<Supplier<List<BlockTintSource>>> colorHandler;

    protected BlockBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        NonNullFunction<BlockBehaviour.Properties, T> factory,
        NonNullSupplier<BlockBehaviour.Properties> initialProperties
    ) {
        super(owner, parent, name, callback, Registries.BLOCK);
        this.factory = factory;
        this.initialProperties = initialProperties;
    }

    /**
     * Modify the properties of the block. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     * <p>
     * If a different properties instance is returned, it will replace the existing one entirely.
     *
     * @param func The action to perform on the properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> properties(NonNullUnaryOperator<BlockBehaviour.Properties> func) {
        propertiesCallback = propertiesCallback.andThen(func);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     *
     * @param block The block to create the initial properties from (via {@link Block.Properties#ofFullCopy(BlockBehaviour)})
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(NonNullSupplier<? extends Block> block) {
        initialProperties = () -> BlockBehaviour.Properties.ofFullCopy(block.get());
        return this;
    }

    // TODO <1.21.5> block layer registration

    /**
     * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's)
     *
     * @return this {@link BlockBuilder}
     * @see #item()
     */
    public BlockBuilder<T, P> simpleItem() {
        return item().defaultLang().build();
    }

    /**
     * Create a standard {@link BlockItem} for this block, and return the builder for it so that further customization can be done.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's)
     *
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public ItemBuilder<BlockItem, BlockBuilder<T, P>> item() {
        return item(BlockItem::new);
    }

    /**
     * Create a {@link BlockItem} for this block, which is created by the given factory, and return the builder for it so that further customization can be done.
     * <p>
     * By default, the item will have no lang entry (since it would duplicate the block's)
     *
     * @param <I>     The type of the item
     * @param factory A factory for the item, which accepts the block object and properties and returns a new item
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public <I extends Item> ItemBuilder<I, BlockBuilder<T, P>> item(NonNullBiFunction<? super T, Item.Properties, ? extends I> factory) {
        return getOwner().<I, BlockBuilder<T, P>>item(this, getName(), p -> factory.apply(getEntry(), p))
            .setData(ProviderType.LANG, NonNullBiConsumer.noop()) // FIXME Need a beetter API for "unsetting" providers
            .model(() -> (ctx, prov) -> {
                var model = getOwner().getDataProvider(ProviderType.BLOCKSTATE)
                    .map(g -> g.seenBlockstates.get(getEntry()))
                    .flatMap(BlockStateModelDispatcher::simpleModels)
                    .map(b -> b.models().get(""))
                    .flatMap(ub -> BlockStateModel.Unbaked.CODEC.encodeStart(JsonOps.INSTANCE, ub).result())
                    .filter(JsonElement::isJsonObject)
                    .map(j -> j.getAsJsonObject().get("model"))
                    .map(JsonElement::getAsString);
                model.ifPresent(s -> prov.createWithExistingModel(ctx.get(), Identifier.parse(s)));
            });
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     *
     * @param <BE>    The type of the block entity
     * @param factory A factory for the block entity
     * @return this {@link BlockBuilder}
     */
    public <BE extends BlockEntity> BlockBuilder<T, P> simpleBlockEntity(BlockEntityFactory<BE> factory) {
        return blockEntity(factory).build();
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * <p>
     * The created {@link BlockEntityBuilder} is returned for further configuration.
     *
     * @param <BE>    The type of the block entity
     * @param factory A factory for the block entity
     * @return the {@link BlockEntityBuilder}
     */
    public <BE extends BlockEntity> BlockEntityBuilder<BE, BlockBuilder<T, P>> blockEntity(BlockEntityFactory<BE> factory) {
        return getOwner().blockEntity(this, getName(), factory).validBlock(asSupplier());
    }

    /**
     * Register a block color handler for this block. The {@link BlockTintSource} instance can be shared across many blocks.
     *
     * @param colorHandler The color handler to register for this block
     * @return this {@link BlockBuilder}
     */
    // TODO it might be worthwhile to abstract this more and add the capability to automatically copy to the item
    public BlockBuilder<T, P> color(NonNullSupplier<Supplier<List<BlockTintSource>>> colorHandler) {
        if (this.colorHandler == null) {
            RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerBlockTintSource);
        }
        this.colorHandler = colorHandler;
        return this;
    }

    protected void registerBlockTintSource() {
        OneTimeEventReceiver.addModListener(
            getOwner(), RegisterColorHandlersEvent.BlockTintSources.class, e -> {
                NonNullSupplier<Supplier<List<BlockTintSource>>> colorHandler = this.colorHandler;
                if (colorHandler != null) {
                    e.register(colorHandler.get().get(), getEntry());
                }
            }
        );
    }

    /**
     * Assign the default blockstate, which maps all states to a single model file (via {@link RegistrumBlockModelGenerator#createTrivialCube(Block)}). This is the default, so it is generally not necessary
     * to call, unless for undoing previous changes.
     *
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultBlockstate() {
        return blockstate(() -> (ctx, prov) -> prov.createTrivialCube(ctx.getEntry()));
    }

    /**
     * Configure the blockstate/models for this block.
     *
     * @param cons The callback which will be invoked during data generation.
     * @return this {@link BlockBuilder}
     * @see #setData(GeneratorType, NonNullBiConsumer)
     */
    public BlockBuilder<T, P> blockstate(NonNullSupplier<NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator>> cons) {
        if (!getOwner().doDatagen().get()) return this;
        return setData(ProviderType.BLOCKSTATE, cons.get());
    }

    /**
     * Assign the default translation, as specified by {@link RegistrumLangProvider#getAutomaticName(NonNullSupplier, net.minecraft.resources.ResourceKey)}. This is the default, so it is generally
     * not necessary to call, unless for undoing previous changes.
     *
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultLang() {
        return lang(Block::getDescriptionId);
    }

    /**
     * Set the translation for this block.
     *
     * @param name A localized English name
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> lang(String name) {
        return lang(Block::getDescriptionId, name);
    }

    /**
     * Assign the default loot table, as specified by {@link RegistrumBlockLootTables#dropSelf(Block)}. This is the default, so it is generally not necessary to call, unless for
     * undoing previous changes.
     *
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultLoot() {
        return loot(RegistrumBlockLootTables::dropSelf);
    }

    /**
     * Configure the loot table for this block. This is different than most data gen callbacks as the callback does not accept a {@link DataGenContext}, but instead a
     * {@link RegistrumBlockLootTables}, for creating specifically block loot tables.
     * <p>
     * If the block does not have a loot table (i.e. {@link Block.Properties#noLootTable()} is called) this action will be <em>skipped</em>.
     *
     * @param cons The callback which will be invoked during block loot table creation.
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> loot(NonNullBiConsumer<RegistrumBlockLootTables, T> cons) {
        return setData(
            ProviderType.LOOT, (ctx, prov) -> prov.addLootAction(
                LootType.BLOCK, tb -> {
                    if (ctx.getEntry().getLootTable().isPresent()) {
                        cons.accept(tb, ctx.getEntry());
                    }
                }
            )
        );
    }

    /**
     * Configure the recipe(s) for this block.
     *
     * @param cons The callback which will be invoked during data generation.
     * @return this {@link BlockBuilder}
     * @see #setData(GeneratorType, NonNullBiConsumer)
     */
    public BlockBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Block, T>, RegistrumRecipeProvider> cons) {
        return setData(ProviderType.RECIPE, cons);
    }

    @Nullable
    private Function<T, NonNullSupplier<Supplier<IClientBlockExtensions>>> clientExtensionFunc;

    /**
     * Register a client extension for this block.
     * The {@link IClientBlockExtensions} instance can be shared across many items.
     *
     * @param clientExtension The client extension to register for this block
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> clientExtension(NonNullSupplier<Supplier<IClientBlockExtensions>> clientExtension) {
        if (this.clientExtensionFunc == null) {
            RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientExtension);
        }
        this.clientExtensionFunc = block -> clientExtension;
        return this;
    }

    /**
     * Register a client extension for this block.
     * The {@link IClientBlockExtensions} instance can be shared across many items.
     *
     * @param clientExtension The client extension to register for this block
     * @return this {@link BlockBuilder}
     */
    @Deprecated(forRemoval = true)
    public BlockBuilder<T, P> clientExtension(Function<T, NonNullSupplier<Supplier<IClientBlockExtensions>>> clientExtension) {
        if (this.clientExtensionFunc == null) {
            RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientExtension);
        }
        this.clientExtensionFunc = clientExtension;
        return this;
    }

    protected void registerClientExtension() {
        OneTimeEventReceiver.addModListener(
            getOwner(), RegisterClientExtensionsEvent.class, e -> {
                if (this.clientExtensionFunc != null) {
                    NonNullSupplier<Supplier<IClientBlockExtensions>> clientExtension = this.clientExtensionFunc.apply(getEntry());
                    e.registerBlock(clientExtension.get().get(), getEntry());
                }
            }
        );
    }

    /**
     * Assign {@link TagKey}{@code s} to this block. Multiple calls will add additional tags.
     *
     * @param tags The tags to assign
     * @return this {@link BlockBuilder}
     */
    @SafeVarargs
    public final BlockBuilder<T, P> tag(TagKey<Block>... tags) {
        return tag(ProviderType.BLOCK_TAGS, tags);
    }

    @Override
    protected T createEntry() {
        BlockBehaviour.Properties properties = this.initialProperties.get();
        //TODO why do we need this?
        // ObfuscationReflectionHelper.setPrivateValue(BlockBehaviour.Properties.class, properties, null, "drops");
        properties = propertiesCallback.apply(properties);
        return factory.apply(properties.setId(getResourceKey()));
    }

    @Override
    protected RegistryEntry<Block, T> createEntryWrapper(DeferredHolder<Block, T> delegate) {
        return new BlockEntry<>(getOwner(), delegate);
    }

    @Override
    public BlockEntry<T> register() {
        return (BlockEntry<T>) super.register();
    }
}
