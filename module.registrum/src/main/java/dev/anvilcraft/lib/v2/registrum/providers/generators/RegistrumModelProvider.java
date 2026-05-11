/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/generators/RegistrateModelProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.generators;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.LogicalSide;

import java.util.stream.Stream;

public class RegistrumModelProvider extends ModelProvider implements RegistrumProvider {

    private final AbstractRegistrum<?> parent;

    public RegistrumModelProvider(AbstractRegistrum<?> parent, PackOutput p_388260_) {
        super(p_388260_, parent.getModid());
        this.parent = parent;
    }

    @Override
    public void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        new RegistrumBlockModelGenerator(parent, blockModels.blockStateOutput, blockModels.itemModelOutput, blockModels.modelOutput).run();
        new RegistrumItemModelGenerator(parent, itemModels.itemModelOutput, itemModels.modelOutput).run();
    }

    @Override
    public Stream<? extends Holder<Block>> getKnownBlocks() {
        return super.getKnownBlocks();
    }

    @Override
    public Stream<? extends Holder<Item>> getKnownItems() {
        return super.getKnownItems();
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

}
