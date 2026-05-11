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

    public void copy(TagKey<Block> p_206422_, TagKey<Item> p_206423_) {
        this.tagsToCopy.put(p_206422_, p_206423_);
    }

    @Override
    protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
        return super.createContentsProvider().thenCombineAsync(
            this.blockTags, (p_274766_, p_274767_) -> {
                this.tagsToCopy.forEach((p_274763_, p_274764_) -> {
                    TagBuilder tagbuilder = this.getOrCreateRawBuilder(p_274764_);
                    Optional<TagBuilder> optional = p_274767_.apply(p_274763_);
                    optional.orElseThrow(() -> new IllegalStateException("Missing block tag " + p_274764_.location()))
                        .build()
                        .forEach(tagbuilder::add);
                });
                return p_274766_;
            }
        );
    }
}
