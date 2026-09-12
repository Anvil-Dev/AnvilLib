package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.BindlessImageArrayBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ComputeBindingLayout;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.AtomicCounterBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ImageArrayBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ImageBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.ShaderStorageBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.TextureBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.bindings.UniformBlockBinding;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ShaderResourceType;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ALRComputePipeline {
    private final Identifier identifier;
    private final List<ComputeBindingLayout<?>> bindings;
    private final Identifier shaderLocation;
    private final ShaderDefines defines;

    private final Map<ShaderResourceType, List<ComputeBindingLayout<?>>> bindingsByType =
        new EnumMap<>(ShaderResourceType.class);

    private final Map<String, ComputeBindingLayout<?>> bindingsByName = new HashMap<>();

    public ALRComputePipeline(
        Identifier identifier,
        List<ComputeBindingLayout<?>> bindings,
        Identifier shaderLocation,
        ShaderDefines defines
    ) {
        this.identifier = identifier;
        this.bindings = bindings;
        this.shaderLocation = shaderLocation;
        this.defines = defines;

        for (ComputeBindingLayout<?> binding : bindings) {
            bindingsByType.computeIfAbsent(
                binding.type(),
                _ -> new ArrayList<>()
            ).add(binding);
            bindingsByName.put(binding.name(), binding);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ALRComputePipeline pipeline) {
        return builder().withPipeline(pipeline);
    }

    public Identifier identifier() {
        return identifier;
    }

    public List<ComputeBindingLayout<?>> bindings() {
        return bindings;
    }

    @Nullable
    public List<ComputeBindingLayout<?>> getBinding(ShaderResourceType type) {
        return this.bindingsByType.get(type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> ComputeBindingLayout<T> getBinding(int bindingPointStart, ShaderResourceType type) {
        List<ComputeBindingLayout<T>> ts = (List<ComputeBindingLayout<T>>) (Object) this.bindingsByType.get(type);

        if (ts == null) {
            return null;
        }

        return ts.get(bindingPointStart);
    }

    @Nullable
    public ComputeBindingLayout<?> getBinding(String name) {
        return this.bindingsByName.get(name);
    }

    public Identifier shaderLocation() {
        return shaderLocation;
    }

    public ShaderDefines defines() {
        return defines;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ALRComputePipeline) obj;
        return Objects.equals(this.identifier, that.identifier) &&
            Objects.equals(this.bindings, that.bindings) &&
            Objects.equals(this.shaderLocation, that.shaderLocation) &&
            Objects.equals(this.defines, that.defines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, bindings, shaderLocation, defines);
    }

    @Override
    public String toString() {
        return "ALRComputePipeline[" +
            "identifier=" + identifier + ", " +
            "bindings=" + bindings + ", " +
            "shaderLocation=" + shaderLocation + ", " +
            "defines=" + defines + ']';
    }

    @Nullable
    public ShaderResourceType getBindingType(String name) {
        ComputeBindingLayout<?> computeBindingLayout = this.bindingsByName.get(name);
        if (computeBindingLayout == null) {
            return null;
        }
        return computeBindingLayout.type();
    }


    public static class Builder {
        private final List<ComputeBindingLayout<?>> bindings = new ArrayList<>();
        private Identifier name;
        private Identifier shaderLocation;
        private ShaderDefines defines = ShaderDefines.EMPTY;

        /// name will not copy from template pipeline
        public Builder withPipeline(ALRComputePipeline pipelineTemplate) {
            this.bindings.clear();
            this.bindings.addAll(pipelineTemplate.bindings());
            this.shaderLocation = pipelineTemplate.shaderLocation();
            this.defines = pipelineTemplate.defines();
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

        /// an array of image2D, not image2DArray
        public Builder withArrayOfImage(String name, boolean read, boolean write, int size) {
            return this.withBinding(new ImageArrayBinding(name, read, write, size));
        }

        public Builder withBindlessArrayOfImage(String name, boolean read, boolean write, int size) {
            return this.withBinding(new BindlessImageArrayBinding(name, read, write, size));
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
                ImmutableList.copyOf(this.bindings),
                Objects.requireNonNull(this.shaderLocation, "shaderLocation"),
                this.defines
            );
        }
    }
}
