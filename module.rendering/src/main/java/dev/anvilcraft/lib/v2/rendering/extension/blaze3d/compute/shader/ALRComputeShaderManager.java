package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.event.RegisterComputePipelinesEvent;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRComputeCapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModLoader;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApiStatus.Internal
public class ALRComputeShaderManager extends SimplePreparableReloadListener<ALRComputeShaderManager.ComputeShaderSource> {
    public static final ALRComputeShaderManager INSTANCE = new ALRComputeShaderManager();

    @UnknownNullability
    private ComputeShaderSource source = null;

    private final Map<ALRComputeProgramInstanceKey, ALRComputeProgramInstance> shaderInstanceMap = new HashMap<>();
    private final Map<ALRComputePipeline, ALRComputeProgramInstance> pipelineToProgramMap = new HashMap<>();

    @Override
    protected @NonNull ComputeShaderSource prepare(ResourceManager manager, ProfilerFiller profiler) {
        ImmutableMap.Builder<Identifier, String> sources = new ImmutableMap.Builder<>();

        Map<Identifier, Resource> shaders = manager.listResources(
            "shaders",
            it -> it.getPath().endsWith(".csh") || it.getPath().endsWith(".compute") || it.getPath().endsWith(".glsl")
        );

        for (Map.Entry<Identifier, Resource> it : shaders.entrySet()) {
            GlslPreprocessor preprocessor = ShaderManager.createPreprocessor(shaders, it.getKey());

            try (Reader reader = it.getValue().openAsReader()) {
                String source = IOUtils.toString(reader);
                List<String> processed = preprocessor.process(source);
                sources.put(it.getKey(), String.join("", processed));
            } catch (IOException ex) {
                log.error("Failed to load compute shader source at {}", it.getKey(), ex);
            }
        }
        log.info("Loaded {} compute shader.", shaders.size());
        return new ComputeShaderSource(sources.build());
    }

    @Override
    protected void apply(ComputeShaderSource preparations, ResourceManager manager, ProfilerFiller profiler) {
        if (!ALRComputeCapabilities.isComputeSupported()) {
            return;
        }
        ALRGpuDeviceExtension deviceExtension = (ALRGpuDeviceExtension) RenderSystem.getDevice();
        for (ALRComputeProgramInstance value : shaderInstanceMap.values()) {
            deviceExtension.alrDestroyComputeShader(value);
        }
        shaderInstanceMap.clear();
        pipelineToProgramMap.clear();

        this.source = preparations;
        RegisterComputePipelinesEvent event = new RegisterComputePipelinesEvent();
        ModLoader.postEvent(event);

        for (ALRComputePipeline pipeline : event.getPipelines()) {
            ALRComputeProgramInstanceKey key = new ALRComputeProgramInstanceKey(
                pipeline.shaderLocation(),
                pipeline.defines()
            );
            log.debug("Compiled COMPUTE shader {}", pipeline.shaderLocation());
            ALRComputeProgramInstance instance = deviceExtension.alrCompileComputePipeline(pipeline, key);
            this.shaderInstanceMap.put(key, instance);
            this.pipelineToProgramMap.put(pipeline, instance);
        }
    }

    public String getSource(Identifier location) {
        return source.source.get(location.withPrefix("shaders/"));
    }

    @Nullable
    public ALRComputeProgramInstance getShader(ALRComputeProgramInstanceKey location) {
        return this.shaderInstanceMap.get(location);
    }

    @Nullable
    public ALRComputeProgramInstance getShader(ALRComputePipeline pipeline) {
        return this.pipelineToProgramMap.get(pipeline);
    }

    public record ComputeShaderSource(
        Map<Identifier, String> source
    ) {
    }
}
