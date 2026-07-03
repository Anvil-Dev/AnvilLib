package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ComputeBindingLayout;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.AtomicCounterBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ImageBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ShaderStorageBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.UniformBlockBinding;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ALRComputePipeline(
    Identifier identifier,
    List<ComputeBindingLayout<?>> bindings,
    Identifier shaderLocation,
    ShaderDefines defines
) {

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ALRComputePipeline pipeline) {
        return builder().withPipeline(pipeline);
    }

    public static class Builder {
        private final List<ComputeBindingLayout<?>> bindings = new ArrayList<>();
        private Identifier name;
        private Identifier shaderLocation;
        private ShaderDefines defines = ShaderDefines.EMPTY;

        public Builder withPipeline(ALRComputePipeline pipeline) {
            this.bindings.clear();
            this.bindings.addAll(pipeline.bindings());
            this.shaderLocation = pipeline.shaderLocation();
            this.defines = pipeline.defines();
            return this;
        }

        public Builder withShader(Identifier shaderLocation) {
            this.shaderLocation = shaderLocation;
            return this;
        }

        public Builder withName(Identifier name) {
            this.name = name;
            return this;
        }

        public Builder withShaderLocation(Identifier shaderLocation) {
            return this.withShader(shaderLocation);
        }

        public Builder withDefines(ShaderDefines defines) {
            this.defines = defines;
            return this;
        }

        public Builder withBinding(ComputeBindingLayout<?> binding) {
            this.bindings.add(binding);
            return this;
        }

        public Builder withTexture(String name) {
            return this.withBinding(new TextureBinding(name));
        }

        public Builder withImage(String name, boolean read, boolean write) {
            return this.withBinding(new ImageBinding(name, read, write));
        }

        public Builder withReadOnlyImage(String name) {
            return this.withImage(name, true, false);
        }

        public Builder withWriteOnlyImage(String name) {
            return this.withImage(name, false, true);
        }

        public Builder withReadWriteImage(String name) {
            return this.withImage(name, true, true);
        }

        public Builder withUniformBlock(String name) {
            return this.withBinding(new UniformBlockBinding(name));
        }

        public Builder withShaderStorage(String name) {
            return this.withBinding(new ShaderStorageBinding(name));
        }

        public Builder withAtomicCounter(String name) {
            return this.withBinding(new AtomicCounterBinding(name));
        }

        public ALRComputePipeline build() {
            return new ALRComputePipeline(
                Objects.requireNonNull(this.name, "name"),
                this.bindings,
                Objects.requireNonNull(this.shaderLocation, "shaderLocation"),
                this.defines
            );
        }
    }
}
