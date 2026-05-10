/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/generators/RegistrateLegacyBlockModelBuilder.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.generators;

import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import net.neoforged.neoforge.client.model.generators.template.RootTransformsBuilder;
import net.neoforged.neoforge.client.model.generators.template.TransformVecBuilder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class RegistrumLegacyBlockModelBuilder {

    private final ExtendedModelTemplateBuilder template;
    private final TextureMapping texture;
    private final BiConsumer<Identifier, ModelInstance> output;

    RegistrumLegacyBlockModelBuilder(
        BiConsumer<Identifier, ModelInstance> output,
        ExtendedModelTemplateBuilder template,
        TextureMapping texture
    ) {
        this.output = output;
        this.template = template;
        this.texture = texture.copy();
    }

    public RegistrumLegacyBlockModelBuilder texture(TextureSlot slot, Identifier texture) {
        return texture(slot, texture, false);
    }

    public RegistrumLegacyBlockModelBuilder texture(TextureSlot slot, Identifier texture, boolean translucent) {
        this.template.requiredTextureSlot(slot);
        this.texture.put(slot, new Material(texture, translucent));
        return this;
    }

    public RegistrumLegacyBlockModelBuilder transformTemplate(Consumer<ExtendedModelTemplateBuilder> action) {
        action.accept(template);
        return this;
    }

    public RegistrumLegacyBlockModelBuilder transformTexture(Consumer<TextureMapping> action) {
        action.accept(texture);
        return this;
    }

    public Identifier build(Block block) {
        return template.build().create(block, texture, output);
    }

    public Identifier build(Identifier loc) {
        return template.build().create(loc, texture, output);
    }

    // Delegated methods from Template Builder

    /**
     * Parent model which this template will inherit its properties from.
     */
    public RegistrumLegacyBlockModelBuilder parent(Identifier parent) {
        template.parent(parent);
        return this;
    }

    /**
     * Suffix appended onto the models file path.
     */
    public RegistrumLegacyBlockModelBuilder suffix(String suffix) {
        template.suffix(suffix);
        return this;
    }

    /**
     * Begin building a new transform for the given perspective.
     *
     * @param type the perspective to create or return the builder for
     * @return the builder for the given perspective
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public RegistrumLegacyBlockModelBuilder transform(ItemDisplayContext type, Consumer<TransformVecBuilder> action) {
        template.transform(type, action);
        return this;
    }

    /**
     * Sets whether or not this model should apply ambient occlusion.
     */
    public RegistrumLegacyBlockModelBuilder ambientOcclusion(boolean ambientOcclusion) {
        template.ambientOcclusion(ambientOcclusion);
        return this;
    }

    /**
     * Sets the gui light style for this model.
     *
     * <ul>
     * <li>{@link UnbakedModel.GuiLight#FRONT} for head on light, commonly used for items.</li>
     * <li>{@link UnbakedModel.GuiLight#SIDE} for the model to be side lit, commonly used for blocks.</li>
     * </ul>
     */
    public RegistrumLegacyBlockModelBuilder guiLight(UnbakedModel.GuiLight light) {
        template.guiLight(light);
        return this;
    }

    /**
     * Use a custom loader instead of the vanilla elements.
     *
     * @param customLoaderFactory function that returns the custom loader to set, given this
     * @return the custom loader builder
     */
    public <L extends CustomLoaderBuilder> RegistrumLegacyBlockModelBuilder customLoader(
        Supplier<L> customLoaderFactory,
        Consumer<L> action
    ) {
        template.customLoader(customLoaderFactory, action);
        return this;
    }

    /**
     * Modifies the transformation applied right before item display transformations and rotations specified in block states.
     */
    public RegistrumLegacyBlockModelBuilder rootTransforms(Consumer<RootTransformsBuilder> action) {
        template.rootTransforms(action);
        return this;
    }

}
