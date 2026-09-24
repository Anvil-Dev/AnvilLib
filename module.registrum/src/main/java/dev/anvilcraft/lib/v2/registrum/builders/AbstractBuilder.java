/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/builders/AbstractBuilder.java
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.LazyRegistryEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class which most builders should extend, instead of implementing [@link {@link Builder} directly.
 * <p>
 * Provides the most basic functionality, and some utility methods that remove the need to pass the registry class.
 *
 * @param <R>
 *            Type of the registry for the current object. This is the concrete base class that all registry entries must extend, and the type used for the forge registry itself.
 * @param <T>
 *            Actual type of the object being built.
 * @param <P>
 *            Type of the parent object, this is returned from {@link #build()} and {@link #getParent()}.
 * @param <S>
 *            Self type
 * @see Builder
 */
@RequiredArgsConstructor
public abstract class AbstractBuilder<R, T extends R, P, S extends AbstractBuilder<R, T, P, S>> implements Builder<R, T, P, S> {

    @Getter(onMethod_ = {@Override})
    private final AbstractRegistrum<?> owner;
    @Getter(onMethod_ = {@Override})
    private final P parent;
    @Getter(onMethod_ = {@Override})
    private final String name;
    @Getter(AccessLevel.PROTECTED)
    private final BuilderCallback callback;
    @Getter(onMethod_ = {@Override})
    private final ResourceKey<? extends Registry<R>> registryKey;

    private final Multimap<ProviderType<? extends RegistrumTagsProvider<?>>, TagKey<?>> tagsByType = HashMultimap.create();

    /** A supplier for the entry that will discard the reference to this builder after it is resolved */
    private final LazyRegistryEntry<R, T> safeSupplier = new LazyRegistryEntry<>(this);

    /** Indicates whether this entry should generate tags as optional tag */
    private boolean isOptional = false;

    /** The names this entry was previously registered under, resolved into aliases by {@link #addAliases(ResourceLocation)} when this builder is registered */
    private final List<ResourceLocation> aliasedFrom = new ArrayList<>();

    /**
     * Create the built entry. This method will be lazily resolved at registration time, so it is safe to bake in values from the builder.
     *
     * @return The built entry
     */
    @SuppressWarnings("null")
    protected abstract @NonnullType T createEntry();

    @Override
    public RegistryEntry<R, T> register() {
        for (ResourceLocation oldName : this.aliasedFrom) {
            addAliases(oldName);
        }
        return callback.accept(name, registryKey, this, this::createEntry, this::createEntryWrapper);
    }

    protected RegistryEntry<R, T> createEntryWrapper(DeferredHolder<R, T> delegate) {
        return new RegistryEntry<>(getOwner(), delegate);
    }

    @Override
    public NonNullSupplier<T> asSupplier() {
        return safeSupplier;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The aliases are not applied immediately: they are recorded and resolved by {@link #addAliases(ResourceLocation)} when this builder is registered, so that builders which only create their derived
     * entries for some configurations can skip aliasing the ones they did not create.
     */
    @SuppressWarnings("unchecked")
    @Override
    public S aliasFrom(ResourceLocation... oldNames) {
        this.aliasedFrom.addAll(Arrays.asList(oldNames));
        return (S) this;
    }

    /**
     * Apply the aliases for every entry this builder registers, given one of the names this builder's entry was previously registered under. Called once per name.
     * <p>
     * Builders which register entries derived from this one under a different name - a block item, a spawn egg, a bucket - override this to alias those registries too. Such an override must only alias
     * the derived entries that were actually created, as the configuration of this builder is final by the time it is called.
     *
     * @param oldName
     *            One of the names this entry used to be registered under
     */
    protected void addAliases(ResourceLocation oldName) {
        getOwner().addAlias(getRegistryKey(), oldName, getEntryId());
    }

    /**
     * The {@link ResourceLocation} this entry is registered under, derived from the owning mod's id and this builder's name.
     *
     * @return the id of this entry
     */
    protected ResourceLocation getEntryId() {
        return ResourceLocation.fromNamespaceAndPath(getOwner().getModid(), getName());
    }

    /**
     * Tag this entry with a tag (or tags) of the correct type. Multiple calls will add additional tags.
     *
     * @param type
     *            The provider type (which must be a tag provider)
     * @param tags
     *            The tags to add
     * @return this {@link Builder}
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <TP extends TagsProvider<R> & RegistrumTagsProvider<R>> S tag(ProviderType<? extends TP> type, TagKey<R>... tags) {
        if (!tagsByType.containsKey(type)) {
            setData(type, (ctx, prov) -> tagsByType.get(type).stream()
                    .map(t -> (TagKey<R>) t)
                    .map(prov::addTag)
                    .forEach(b -> b.add(asTag())));
        }
        tagsByType.putAll(type, Arrays.asList(tags));
        return (S) this;
    }

    /**
     * Mark this entry as optional when generating tags
     * */
    @SuppressWarnings("unchecked")
    public S asOptional(){
        isOptional = true;
        return (S) this;
    }

    protected TagEntry asTag() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(getOwner().getModid(), getName());
        if (isOptional) return TagEntry.optionalElement(id);
        return TagEntry.element(id);
    }

    /**
     * Remove a tag (or tags) from this entry of a given type. Useful to remove default tags on fluids, for example. Multiple calls will remove additional tags.
     *
     * @param type
     *            The provider type (which must be a tag provider)
     * @param tags
     *            The tags to remove
     * @return this {@link Builder}
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <TP extends TagsProvider<R> & RegistrumTagsProvider<R>> S removeTag(ProviderType<TP> type, TagKey<R>... tags) {
        if (tagsByType.containsKey(type)) {
            for (TagKey<R> tag : tags) {
                tagsByType.remove(type, tag);
            }
        }
        return (S) this;
    }

    /**
     * Set the lang key for this entry to the default value (specified by {@link RegistrumLangProvider#getAutomaticName(NonNullSupplier, ResourceKey)}). Generally, specific helpers from concrete
     * builders should be used instead.
     *
     * @param langKeyProvider
     *            A function to get the translation key from the entry
     * @return this {@link Builder}
     */
    public S lang(NonNullFunction<T, String> langKeyProvider) {
        return lang(langKeyProvider, (p, t) -> p.<R>getAutomaticName(t, getRegistryKey()));
    }

    /**
     * Set the lang key for this entry to the specified name. Generally, specific helpers from concrete builders should be used instead.
     *
     * @param langKeyProvider
     *            A function to get the translation key from the entry
     * @param name
     *            The name to use
     * @return this {@link Builder}
     */
    public S lang(NonNullFunction<T, String> langKeyProvider, String name) {
        return lang(langKeyProvider, (p, s) -> name);
    }

    private S lang(NonNullFunction<T, String> langKeyProvider, NonNullBiFunction<RegistrumLangProvider, NonNullSupplier<? extends T>, String> localizedNameProvider) {
        return setData(ProviderType.LANG, (ctx, prov) -> prov.add(langKeyProvider.apply(ctx.getEntry()), localizedNameProvider.apply(prov, ctx::getEntry)));
    }
}
