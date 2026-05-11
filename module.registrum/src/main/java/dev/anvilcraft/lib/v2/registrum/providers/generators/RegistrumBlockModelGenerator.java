/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/generators/RegistrateBlockModelGenerator.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.generators;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RegistrumBlockModelGenerator extends BlockModelGenerators {

    private final AbstractRegistrum<?> parent;
    public final Map<Block, BlockStateModelDispatcher> seenBlockstates = new HashMap<>();

    public RegistrumBlockModelGenerator(
        AbstractRegistrum<?> parent,
        Consumer<BlockModelDefinitionGenerator> known,
        ItemModelOutput item,
        BiConsumer<Identifier, ModelInstance> model
    ) {
        super(known, item, model);
        ObfuscationReflectionHelper.<BlockModelGenerators, Consumer<BlockModelDefinitionGenerator>>setPrivateValue(
            BlockModelGenerators.class, this, g -> {
                this.seenBlockstates.put(g.block(), g.create());
                known.accept(g);
            }, "blockStateOutput"
        );
        this.parent = parent;
    }

    @Override
    public void run() {
        parent.genData(ProviderType.BLOCKSTATE, this);
    }

    public void create(Block block, Identifier model) {
        this.blockStateOutput.accept(createSimpleBlock(block, plainVariant(model)));
    }

    public void create(Block block, TexturedModel.Provider texture) {
        this.blockStateOutput.accept(createSimpleBlock(block, plainVariant(texture.create(block, this.modelOutput))));
    }

    public Identifier mcLoc(String id) {
        return Identifier.withDefaultNamespace(id);
    }

    public Identifier modLoc(String id) {
        return Identifier.fromNamespaceAndPath(parent.getModid(), id);
    }

    public RegistrumLegacyBlockModelBuilder withBuilder(ExtendedModelTemplateBuilder template, TextureMapping texture) {
        return new RegistrumLegacyBlockModelBuilder(modelOutput, template, texture);
    }

    public RegistrumLegacyBlockModelBuilder withBuilder(ExtendedModelTemplateBuilder template) {
        return withBuilder(template, new TextureMapping());
    }

    public RegistrumLegacyBlockModelBuilder getBuilder() {
        return withBuilder(new ExtendedModelTemplateBuilder());
    }

    public RegistrumLegacyBlockModelBuilder withParent(ModelTemplate template) {
        return withBuilder(ExtendedModelTemplateBuilder.of(template));
    }

    public RegistrumLegacyBlockModelBuilder withParent(ModelTemplate template, TextureMapping texture) {
        return withBuilder(ExtendedModelTemplateBuilder.of(template), texture);
    }

    public RegistrumLegacyBlockModelBuilder withParent(TexturedModel model) {
        return withBuilder(ExtendedModelTemplateBuilder.of(model.getTemplate()), model.getMapping());
    }
}
