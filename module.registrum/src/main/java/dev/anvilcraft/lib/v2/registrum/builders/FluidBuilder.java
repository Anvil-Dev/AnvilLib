/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/builders/FluidBuilder.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.anvilcraft.lib.v2.registrum.util.OneTimeEventReceiver;
import dev.anvilcraft.lib.v2.registrum.util.RegistrumDistExecutor;
import dev.anvilcraft.lib.v2.registrum.util.entry.FluidEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

@SuppressWarnings("unused")
public class FluidBuilder<T extends BaseFlowingFluid, P> extends AbstractBuilder<Fluid, T, P, FluidBuilder<T, P>> {

    @FunctionalInterface
    public interface FluidTypeFactory {
        FluidType create(FluidType.Properties properties);
    }

    @FunctionalInterface
    public interface FluidFactory<T> {
        T create(BaseFlowingFluid.Properties properties);
    }

    @Nullable
    private NonNullSupplier<Supplier<IClientFluidTypeExtensions>> clientExtension;

    /**
     * Register a client extension for this block. The {@link IClientBlockExtensions} instance can be shared across many items.
     *
     * @param clientExtension The client extension to register for this block
     * @return this {@link BlockBuilder}
     */
    public FluidBuilder<T, P> clientExtension(NonNullSupplier<Supplier<IClientFluidTypeExtensions>> clientExtension) {
        if (this.clientExtension == null) {
            RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientExtension);
        }
        this.clientExtension = clientExtension;
        return this;
    }

    protected void registerClientExtension() {
        OneTimeEventReceiver.addModListener(
            getOwner(), RegisterClientExtensionsEvent.class, e -> {
                NonNullSupplier<Supplier<IClientFluidTypeExtensions>> clientExtension = this.clientExtension;
                if (clientExtension != null) {
                    IClientFluidTypeExtensions extensions = clientExtension.get().get();
                    NonNullSupplier<FluidType> fluidType = this.fluidType;
                    //noinspection ConstantValue
                    if (extensions != null && fluidType != null) {
                        e.registerFluidType(extensions, fluidType.get());
                    }
                }
            }
        );
    }

    @Nullable
    private NonNullSupplier<Supplier<DefaultFluidModel>> fluidModel;

    public FluidBuilder<T, P> fluidModel(NonNullSupplier<Supplier<DefaultFluidModel>> fluidModel) {
        if (this.fluidModel == null) {
            RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerFluidModel);
        }
        this.fluidModel = fluidModel;
        return this;
    }

    public FluidBuilder<T, P> fluidModel(Identifier stillTexture, Identifier flowingTexture) {
        return fluidModel(() -> () -> new DefaultFluidModel(stillTexture, true, flowingTexture, true, null, false, null));
    }

    protected void registerFluidModel() {
        OneTimeEventReceiver.addModListener(
            getOwner(), RegisterFluidModelsEvent.class, e -> {
                NonNullSupplier<Supplier<DefaultFluidModel>> fluidModel = this.fluidModel;
                NonNullSupplier<? extends BaseFlowingFluid> fluid = this.source;
                if (fluidModel != null && fluid != null) {
                    DefaultFluidModel model = fluidModel.get().get();
                    e.register(
                        new FluidModel.Unbaked(
                            new Material(
                                model.stillTexture(),
                                model.stillTranslucent()
                            ),
                            new Material(
                                model.flowingTexture(),
                                model.flowingTranslucent()
                            ),
                            model.overlayTexture() == null ? null : new Material(
                                model.overlayTexture(),
                                model.overlayTranslucent()
                            ),
                            model.fluidTintSource()
                        ),
                        fluid.get()
                    );
                }
            }
        );
    }

    /**
     * Create a new {@link FluidBuilder} and configure data. The created builder will use a default ({@link FluidType}) and fluid class ({@link BaseFlowingFluid.Flowing}).
     *
     * @param <P>      Parent object type
     * @param owner    The owning {@link AbstractRegistrum} object
     * @param parent   The parent object
     * @param name     Name of the entry being built
     * @param callback A callback used to actually register the built entry
     * @return A new {@link FluidBuilder} with reasonable default data generators.
     * @see #create(AbstractRegistrum, Object, String, BuilderCallback, FluidTypeFactory, FluidFactory)
     */
    public static <P> FluidBuilder<BaseFlowingFluid.Flowing, P> create(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback
    ) {
        return create(owner, parent, name, callback, FluidType::new, BaseFlowingFluid.Flowing::new);
    }

    /**
     * Create a new {@link FluidBuilder} and configure data. The created builder will use a default fluid class ({@link BaseFlowingFluid.Flowing}).
     *
     * @param <P>         Parent object type
     * @param owner       The owning {@link AbstractRegistrum} object
     * @param parent      The parent object
     * @param name        Name of the entry being built
     * @param callback    A callback used to actually register the built entry
     * @param typeFactory A factory that creates the fluid type
     * @return A new {@link FluidBuilder} with reasonable default data generators.
     * @see #create(AbstractRegistrum, Object, String, BuilderCallback, FluidTypeFactory, FluidFactory)
     */
    public static <P> FluidBuilder<BaseFlowingFluid.Flowing, P> create(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        FluidTypeFactory typeFactory
    ) {
        return create(owner, parent, name, callback, typeFactory, BaseFlowingFluid.Flowing::new);
    }

    /**
     * Create a new {@link FluidBuilder} and configure data. The created builder will use a default fluid class ({@link BaseFlowingFluid.Flowing}).
     *
     * @param <P>       Parent object type
     * @param owner     The owning {@link AbstractRegistrum} object
     * @param parent    The parent object
     * @param name      Name of the entry being built
     * @param callback  A callback used to actually register the built entry
     * @param fluidType An existing and registered fluid type.
     * @return A new {@link FluidBuilder} with reasonable default data generators.
     * @see #create(AbstractRegistrum, Object, String, BuilderCallback, FluidTypeFactory, FluidFactory)
     */
    public static <P> FluidBuilder<BaseFlowingFluid.Flowing, P> create(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        NonNullSupplier<FluidType> fluidType
    ) {
        return create(owner, parent, name, callback, p -> fluidType.get(), BaseFlowingFluid.Flowing::new);
    }

    /**
     * Create a new {@link FluidBuilder} and configure data. The created builder will use a default ({@link FluidType}) and fluid class ({@link BaseFlowingFluid.Flowing}).
     *
     * @param <T>          The type of the builder
     * @param <P>          Parent object type
     * @param owner        The owning {@link AbstractRegistrum} object
     * @param parent       The parent object
     * @param name         Name of the entry being built
     * @param callback     A callback used to actually register the built entry
     * @param fluidFactory A factory that creates the flowing fluid
     * @return A new {@link FluidBuilder} with reasonable default data generators.
     */
    public static <T extends BaseFlowingFluid, P> FluidBuilder<T, P> create(
        AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback,
        FluidFactory<T> fluidFactory
    ) {
        return create(owner, parent, name, callback, FluidType::new, fluidFactory);
    }

    /**
     * Create a new {@link FluidBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The fluid will be assigned the following data:
     * <ul>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * <li>A default {@link BaseFlowingFluid.Source source fluid} (via {@link #defaultSource})</li>
     * <li>A default block for the fluid, with its own default blockstate and model that configure the particle texture (via {@link #defaultBlock()})</li>
     * <li>A default bucket item, that uses a simple generated item model with a texture of the same name as this fluid (via {@link #defaultBucket()})</li>
     * </ul>
     *
     * @param <T>          The type of the builder
     * @param <P>          Parent object type
     * @param owner        The owning {@link AbstractRegistrum} object
     * @param parent       The parent object
     * @param name         Name of the entry being built
     * @param callback     A callback used to actually register the built entry
     * @param typeFactory  A factory that creates the fluid type
     * @param fluidFactory A factory that creates the flowing fluid
     * @return A new {@link FluidBuilder} with reasonable default data generators.
     */
    public static <T extends BaseFlowingFluid, P> FluidBuilder<T, P> create(
        AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback,
        FluidTypeFactory typeFactory, FluidFactory<T> fluidFactory
    ) {
        return new FluidBuilder<>(owner, parent, name, callback, typeFactory, fluidFactory)
            .defaultLang().defaultSource().defaultBlock().defaultBucket();
    }

    /**
     * Create a new {@link FluidBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The fluid will be assigned the following data:
     * <ul>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * <li>A default {@link BaseFlowingFluid.Source source fluid} (via {@link #defaultSource})</li>
     * <li>A default block for the fluid, with its own default blockstate and model that configure the particle texture (via {@link #defaultBlock()})</li>
     * <li>A default bucket item, that uses a simple generated item model with a texture of the same name as this fluid (via {@link #defaultBucket()})</li>
     * </ul>
     *
     * @param <T>          The type of the builder
     * @param <P>          Parent object type
     * @param owner        The owning {@link AbstractRegistrum} object
     * @param parent       The parent object
     * @param name         Name of the entry being built
     * @param callback     A callback used to actually register the built entry
     * @param fluidType    An existing and registered fluid type
     * @param fluidFactory A factory that creates the flowing fluid
     * @return A new {@link FluidBuilder} with reasonable default data generators.
     */
    public static <T extends BaseFlowingFluid, P> FluidBuilder<T, P> create(
        AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback,
        NonNullSupplier<FluidType> fluidType, FluidFactory<T> fluidFactory
    ) {
        return new FluidBuilder<>(owner, parent, name, callback, fluidType, fluidFactory)
            .defaultLang().defaultSource().defaultBlock().defaultBucket();
    }

    private final String sourceName, bucketName;

    private final FluidFactory<T> fluidFactory;

    @Nullable
    private final NonNullSupplier<FluidType> fluidType;

    @Nullable
    private Boolean defaultSource, defaultBlock, defaultBucket;

    private NonNullConsumer<FluidType.Properties> typeProperties = _ -> {
    };

    private NonNullConsumer<BaseFlowingFluid.Properties> fluidProperties;

    private final @Nullable Supplier<Supplier<ChunkSectionLayer>> layer = null;

    private final boolean registerType;

    @Nullable
    private NonNullSupplier<? extends BaseFlowingFluid> source;
    private final List<TagKey<Fluid>> tags = new ArrayList<>();

    public FluidBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        FluidTypeFactory typeFactory,
        FluidFactory<T> fluidFactory
    ) {
        super(owner, parent, "flowing_" + name, callback, Registries.FLUID);
        this.sourceName = name;
        this.bucketName = name + "_bucket";
        this.fluidFactory = fluidFactory;
        this.fluidType = NonNullSupplier.lazy(() -> typeFactory.create(makeTypeProperties()));
        this.registerType = true;

        String bucketName = this.bucketName;
        this.fluidProperties = p -> p.bucket(() -> owner.get(bucketName, Registries.ITEM).get())
            .block(() -> owner.<Block, LiquidBlock>get(name, Registries.BLOCK).get());
    }

    public FluidBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        NonNullSupplier<FluidType> fluidType,
        FluidFactory<T> fluidFactory
    ) {
        super(owner, parent, "flowing_" + name, callback, Registries.FLUID);
        this.sourceName = name;
        this.bucketName = name + "_bucket";
        this.fluidFactory = fluidFactory;
        this.fluidType = fluidType;
        this.registerType = false; // Don't register if we have a fluid from outside.

        String bucketName = this.bucketName;
        this.fluidProperties = p -> p.bucket(() -> owner.get(bucketName, Registries.ITEM).get())
            .block(() -> owner.<Block, LiquidBlock>get(name, Registries.BLOCK).get());
    }

    /**
     * Modify the properties of the fluid type. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     *
     * @param cons The action to perform on the attributes
     * @return this {@link FluidBuilder}
     */
    public FluidBuilder<T, P> properties(NonNullConsumer<FluidType.Properties> cons) {
        typeProperties = typeProperties.andThen(cons);
        return this;
    }

    /**
     * Modify the properties of the flowing fluid. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     *
     * @param cons The action to perform on the attributes
     * @return this {@link FluidBuilder}
     */
    public FluidBuilder<T, P> fluidProperties(NonNullConsumer<BaseFlowingFluid.Properties> cons) {
        fluidProperties = fluidProperties.andThen(cons);
        return this;
    }

    /**
     * Assign the default translation, as specified by {@link RegistrumLangProvider#toEnglishName(String)}. This is the default, so it is generally not necessary to call, unless for
     * undoing previous changes.
     *
     * @return this {@link FluidBuilder}
     */
    public FluidBuilder<T, P> defaultLang() {
        return lang(f -> f.getFluidType().getDescriptionId(), RegistrumLangProvider.toEnglishName(sourceName));
    }

    /**
     * Set the translation for this fluid.
     *
     * @param name A localized English name
     * @return this {@link FluidBuilder}
     */
    public FluidBuilder<T, P> lang(String name) {
        return lang(f -> f.getFluidType().getDescriptionId(), name);
    }

    /**
     * Create a standard {@link BaseFlowingFluid.Source} for this fluid which will be built and registered along with this fluid.
     *
     * @return this {@link FluidBuilder}
     * @throws IllegalStateException If {@link #source(NonNullFunction)} has been called before this method
     * @see #source(NonNullFunction)
     */
    public FluidBuilder<T, P> defaultSource() {
        if (this.defaultSource != null) {
            throw new IllegalStateException("Cannot set a default source after a custom source has been created");
        }
        this.defaultSource = true;
        return this;
    }

    /**
     * Create a {@link BaseFlowingFluid} for this fluid, which is created by the given factory, and which will be built and registered along with this fluid.
     *
     * @param factory A factory for the fluid, which accepts the properties and returns a new fluid
     * @return this {@link FluidBuilder}
     */
    public FluidBuilder<T, P> source(NonNullFunction<BaseFlowingFluid.Properties, ? extends BaseFlowingFluid> factory) {
        this.defaultSource = false;
        this.source = NonNullSupplier.lazy(() -> factory.apply(makeProperties()));
        return this;
    }

    /**
     * Create a standard {@link LiquidBlock} for this fluid, building it immediately, and not allowing for further configuration.
     *
     * @return this {@link FluidBuilder}
     * @throws IllegalStateException If {@link #block()} or {@link #block(NonNullBiFunction)} has been called before this method
     * @see #block()
     */
    public FluidBuilder<T, P> defaultBlock() {
        if (this.defaultBlock != null) {
            throw new IllegalStateException("Cannot set a default block after a custom block has been created");
        }
        this.defaultBlock = true;
        return this;
    }

    /**
     * Create a standard {@link LiquidBlock} for this fluid, and return the builder for it so that further customization can be done.
     *
     * @return the {@link BlockBuilder} for the {@link LiquidBlock}
     */
    public BlockBuilder<LiquidBlock, FluidBuilder<T, P>> block() {
        return block(LiquidBlock::new);
    }

    /**
     * Create a {@link LiquidBlock} for this fluid, which is created by the given factory, and return the builder for it so that further customization can be done.
     *
     * @param <B>     The type of the block
     * @param factory A factory for the block, which accepts the block object and properties and returns a new block
     * @return the {@link BlockBuilder} for the {@link LiquidBlock}
     */
    public <B extends LiquidBlock> BlockBuilder<B, FluidBuilder<T, P>> block(NonNullBiFunction<T, BlockBehaviour.Properties, ? extends B> factory) {
        if (this.defaultBlock == Boolean.FALSE) {
            throw new IllegalStateException("Only one call to block/noBlock per builder allowed");
        }
        this.defaultBlock = false;
        final NonNullSupplier<T> supplier = asSupplier();
        final var lightLevel = Lazy.of(() -> Objects.requireNonNull(fluidType).get().getLightLevel());
        final ToIntFunction<BlockState> lightLevelInt = $ -> lightLevel.get();
        return getOwner().<B, FluidBuilder<T, P>>block(this, sourceName, p -> factory.apply(supplier.get(), p))
            .properties(p -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable())
            .properties(p -> p.lightLevel(lightLevelInt))
            .blockstate(() -> (ctx, prov) -> prov.createNonTemplateModelBlock(ctx.get()));
    }

    @Beta
    public FluidBuilder<T, P> noBlock() {
        if (this.defaultBlock == Boolean.FALSE) {
            throw new IllegalStateException("Only one call to block/noBlock per builder allowed");
        }
        this.defaultBlock = false;
        return this;
    }

    /**
     * Create a standard {@link BucketItem} for this fluid, building it immediately, and not allowing for further configuration.
     *
     * @return this {@link FluidBuilder}
     * @throws IllegalStateException If {@link #bucket()} or {@link #bucket(NonNullBiFunction)} has been called before this method
     * @see #bucket()
     */
    public FluidBuilder<T, P> defaultBucket() {
        if (this.defaultBucket != null) {
            throw new IllegalStateException("Cannot set a default bucket after a custom bucket has been created");
        }
        defaultBucket = true;
        return this;
    }

    /**
     * Create a standard {@link BucketItem} for this fluid, and return the builder for it so that further customization can be done.
     *
     * @return the {@link ItemBuilder} for the {@link BucketItem}
     */
    public ItemBuilder<BucketItem, FluidBuilder<T, P>> bucket() {
        return bucket(BucketItem::new);
    }

    /**
     * Create a {@link BucketItem} for this fluid, which is created by the given factory, and return the builder for it so that further customization can be done.
     *
     * @param <I>     The type of the bucket item
     * @param factory A factory for the bucket item, which accepts the fluid object supplier and properties and returns a new item
     * @return the {@link ItemBuilder} for the {@link BucketItem}
     */
    public <I extends BucketItem> ItemBuilder<I, FluidBuilder<T, P>> bucket(NonNullBiFunction<BaseFlowingFluid, Item.Properties, ? extends I> factory) {
        if (this.defaultBucket == Boolean.FALSE) {
            throw new IllegalStateException("Only one call to bucket/noBucket per builder allowed");
        }
        this.defaultBucket = false;
        NonNullSupplier<? extends BaseFlowingFluid> source = this.source;
        // TODO: Can we find a way to circumvent this limitation?
        if (source == null) {
            throw new IllegalStateException("Cannot create a bucket before creating a source block");
        }
        return getOwner().<I, FluidBuilder<T, P>>item(this, bucketName, p -> factory.apply(source.get(), p))
            .properties(p -> p.craftRemainder(Items.BUCKET).stacksTo(1))
            .model(() -> (ctx, prov) -> prov.generateFlatItem(ctx.get(), ModelTemplates.FLAT_ITEM));
    }

    @Beta
    public FluidBuilder<T, P> noBucket() {
        if (this.defaultBucket == Boolean.FALSE) {
            throw new IllegalStateException("Only one call to bucket/noBucket per builder allowed");
        }
        this.defaultBucket = false;
        return this;
    }

    /**
     * Assign {@link TagKey}{@code s} to this fluid and its source fluid. Multiple calls will add additional tags.
     *
     * @param tags The tags to assign
     * @return this {@link FluidBuilder}
     */
    @SafeVarargs
    public final FluidBuilder<T, P> tag(TagKey<Fluid>... tags) {
        FluidBuilder<T, P> ret = this.tag(ProviderType.FLUID_TAGS, tags);
        if (this.tags.isEmpty()) {
            ret.getOwner().<RegistrumTagsProvider.Intrinsic<Fluid>, Fluid>setDataGenerator(
                ret.sourceName, getRegistryKey(), ProviderType.FLUID_TAGS,
                prov -> this.tags.stream().map(prov::tag).forEach(p -> p.add(getSource()))
            );
        }
        this.tags.addAll(Arrays.asList(tags));
        return ret;
    }

    /**
     * Remove {@link TagKey}{@code s} from this fluid and its source fluid. Multiple calls will remove additional tags.
     *
     * @param tags The tags to remove
     * @return this {@link FluidBuilder}
     */
    @SafeVarargs
    public final FluidBuilder<T, P> removeTag(TagKey<Fluid>... tags) {
        this.tags.removeAll(Arrays.asList(tags));
        return this.removeTag(ProviderType.FLUID_TAGS, tags);
    }

    private BaseFlowingFluid getSource() {
        NonNullSupplier<? extends BaseFlowingFluid> source = this.source;
        Preconditions.checkNotNull(source, "Fluid has no source block: " + sourceName);
        return source.get();
    }

    private BaseFlowingFluid.Properties makeProperties() {
        NonNullSupplier<? extends BaseFlowingFluid> source = this.source;
        @SuppressWarnings("DataFlowIssue")
        BaseFlowingFluid.Properties ret = new BaseFlowingFluid.Properties(Objects.requireNonNull(fluidType), source, asSupplier());
        fluidProperties.accept(ret);
        return ret;
    }

    private FluidType.Properties makeTypeProperties() {
        FluidType.Properties properties = FluidType.Properties.create();
        Optional<RegistryEntry<Block, Block>> block = getOwner().getOptional(sourceName, Registries.BLOCK);
        this.typeProperties.accept(properties);

        // Force the translation key after the user callback runs
        // This is done because we need to remove the lang data generator if using the block key,
        // and if it was possible to undo this change, it might result in the user translation getting
        // silently lost, as there's no good way to check whether the translation key was changed.
        // TODO improve this?
        if (block.isPresent() && block.get().isBound()) {
            properties.descriptionId(block.get().get().getDescriptionId());
            setData(ProviderType.LANG, NonNullBiConsumer.noop());
        } else {
            properties.descriptionId(Util.makeDescriptionId(
                "fluid",
                Identifier.fromNamespaceAndPath(getOwner().getModid(), sourceName)
            ));
        }

        return properties;
    }

    @Override
    protected T createEntry() {
        return fluidFactory.create(makeProperties());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Additionally registers the source fluid and the fluid type (if constructed).
     */
    @SuppressWarnings(
        {
            "unchecked",
            "rawtypes"
        }
    )
    @Override
    public FluidEntry<T> register() {
        // Check the fluid has a type.
        if (this.fluidType != null) {
            // Register the type.
            if (this.registerType) {
                getOwner().simple(this, this.sourceName, NeoForgeRegistries.Keys.FLUID_TYPES, this.fluidType);
            }
        } else {
            throw new IllegalStateException("Fluid must have a type: " + getName());
        }

        if (defaultSource == Boolean.TRUE) {
            source(BaseFlowingFluid.Source::new);
        }
        if (defaultBlock == Boolean.TRUE) {
            block().register();
        }
        if (defaultBucket == Boolean.TRUE) {
            bucket().register();
        }

        NonNullSupplier<? extends BaseFlowingFluid> source = this.source;
        if (source != null) {
            getCallback().accept(sourceName, Registries.FLUID, (FluidBuilder) this, source);
        } else {
            throw new IllegalStateException("Fluid must have a source version: " + getName());
        }

        return (FluidEntry<T>) super.register();
    }

    @Override
    protected RegistryEntry<Fluid, T> createEntryWrapper(DeferredHolder<Fluid, T> delegate) {
        return new FluidEntry<>(getOwner(), delegate);
    }

    public record DefaultFluidModel(
        Identifier stillTexture,
        boolean stillTranslucent,
        Identifier flowingTexture,
        boolean flowingTranslucent,
        @Nullable Identifier overlayTexture,
        boolean overlayTranslucent,
        @Nullable FluidTintSource fluidTintSource
    ) {
    }
}
