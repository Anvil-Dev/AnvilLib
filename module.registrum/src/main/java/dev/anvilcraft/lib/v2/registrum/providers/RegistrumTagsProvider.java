/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateTagsProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.LogicalSide;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface RegistrumTagsProvider<T> extends RegistrumLookupFillerProvider {

    CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter();

    ResourceKey<? extends Registry<T>> registry();

    TagBuilder rawBuilder(TagKey<T> key);

    interface Key<T> extends RegistrumTagsProvider<T> {
        TagAppender<T> tag(TagKey<T> key);
    }

    interface Intrinsic<T> extends RegistrumTagsProvider<T> {
        IntrinsicAppender<T> tag(TagKey<T> key);
    }

    class Impl<T> extends TagsProvider<T> implements RegistrumTagsProvider.Key<T> {
        private final AbstractRegistrum<?> owner;
        private final ProviderType<? extends Impl<T>> type;
        private final String name;

        public Impl(AbstractRegistrum<?> owner, ProviderType<? extends Impl<T>> type, String name, PackOutput packOutput, ResourceKey<? extends Registry<T>> registryIn, CompletableFuture<HolderLookup.Provider> registriesLookup) {
            super(packOutput, registryIn, registriesLookup, owner.getModid());

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public TagBuilder rawBuilder(final TagKey<T> key) {
            return super.getOrCreateRawBuilder(key);
        }

        @Override
        public TagAppender<T> tag(TagKey<T> key) {
            return super.tag(key);
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

        @Override
        public ResourceKey<? extends Registry<T>> registry() {
            return registryKey;
        }

    }

    class IntrinsicImpl<T> extends TagsProvider<T> implements RegistrumTagsProvider.Intrinsic<T> {
        private final AbstractRegistrum<?> owner;
        private final ProviderType<? extends IntrinsicImpl<T>> type;
        private final String name;
        private final Function<T, ResourceKey<T>> keyExtractor;

        public IntrinsicImpl(AbstractRegistrum<?> owner, ProviderType<? extends IntrinsicImpl<T>> type, String name, PackOutput packOutput, ResourceKey<? extends Registry<T>> registryIn, CompletableFuture<HolderLookup.Provider> registriesLookup, Function<T, ResourceKey<T>> keyExtractor) {
            super(packOutput, registryIn, registriesLookup, owner.getModid());
            this.keyExtractor = keyExtractor;

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public TagBuilder rawBuilder(TagKey<T> key) {
            return super.getOrCreateRawBuilder(key);
        }

        @Override
        public IntrinsicAppender<T> tag(final TagKey<T> key) {
            return new IntrinsicAppender<>(super.tag(key), keyExtractor);
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

        @Override
        public ResourceKey<? extends Registry<T>> registry() {
            return registryKey;
        }
    }

    /** Adds registry values while retaining Minecraft's key-based tag API. */
    record IntrinsicAppender<T>(TagAppender<T> delegate, Function<T, ResourceKey<T>> keyExtractor) implements TagAppender<T> {
        public IntrinsicAppender<T> add(T value) { return add(keyExtractor.apply(value)); }
        @SafeVarargs
        public final IntrinsicAppender<T> add(T... values) {
            for (T value : values) add(value);
            return this;
        }
        @Override
        public IntrinsicAppender<T> add(ResourceKey<T> key) { delegate.add(key); return this; }
        @Override
        public IntrinsicAppender<T> addOptional(ResourceKey<T> key) { delegate.addOptional(key); return this; }
        @Override
        public IntrinsicAppender<T> addTag(TagKey<T> tag) { delegate.addTag(tag); return this; }
        @Override
        public IntrinsicAppender<T> addOptionalTag(TagKey<T> tag) { delegate.addOptionalTag(tag); return this; }
        @Override
        public IntrinsicAppender<T> add(net.minecraft.tags.TagEntry entry) { delegate.add(entry); return this; }
        @Override
        public IntrinsicAppender<T> replace(boolean replace) { delegate.replace(replace); return this; }
        @Override
        public IntrinsicAppender<T> remove(ResourceKey<T> key) { delegate.remove(key); return this; }
        @Override
        public IntrinsicAppender<T> remove(TagKey<T> tag) { delegate.remove(tag); return this; }
    }
}
