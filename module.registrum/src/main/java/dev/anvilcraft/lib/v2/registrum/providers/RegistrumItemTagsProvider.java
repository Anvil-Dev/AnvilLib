/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateItemTagsProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RegistrumItemTagsProvider extends RegistrumTagsProvider.IntrinsicImpl<Item> {

    private final CompletableFuture<TagsProvider.TagLookup<Block>> blockTags;
    private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy = new HashMap<>();

    @SuppressWarnings("deprecation")
    public RegistrumItemTagsProvider(
        AbstractRegistrum<?> owner,
        ProviderType<RegistrumItemTagsProvider> type,
        String name,
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesLookup,
        CompletableFuture<TagsProvider.TagLookup<Block>> blockTags
    ) {
        super(owner, type, name, output, Registries.ITEM, registriesLookup, item -> item.builtInRegistryHolder().key());
        this.blockTags = blockTags;
    }

    public void copy(TagKey<Block> blockTagKey, TagKey<Item> itemTagKey) {
        this.tagsToCopy.put(blockTagKey, itemTagKey);
    }

    @Override
    protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
        return super.createContentsProvider().thenCombineAsync(
            this.blockTags, (provider, blockTagLookup) -> {
                this.tagsToCopy.forEach((blockTagKey, itemTagKey) -> {
                    TagBuilder tagbuilder = this.getOrCreateRawBuilder(itemTagKey);
                    Optional<TagBuilder> optional = blockTagLookup.apply(blockTagKey);
                    optional.orElseThrow(() -> new IllegalStateException("Missing block tag " + itemTagKey.location()))
                        .build()
                        .forEach(tagbuilder::add);
                });
                return provider;
            }
        );
    }
}
