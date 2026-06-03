package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.event.RegisterComputePipelinesEvent;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
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
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ALRComputeShaderManager extends SimplePreparableReloadListener<ALRComputeShaderManager.ComputeShaderSource> {
    public static final ALRComputeShaderManager INSTANCE = new ALRComputeShaderManager();

    @UnknownNullability
    private ComputeShaderSource source = null;

    private Map<ALRComputeShaderInstanceKey, ALRComputeShaderInstance> shaderInstanceMap = new HashMap<>();

    @Override
    protected ComputeShaderSource prepare(ResourceManager manager, ProfilerFiller profiler) {
        ImmutableMap.Builder<Identifier, String> sources = new ImmutableMap.Builder<>();

        Map<Identifier, Resource> shaders = manager.listResources(
            "shaders",
            it -> it.getPath().endsWith(".csh") || it.getPath().endsWith(".compute") || it.getPath().endsWith(".glsl")
        );

        for (Map.Entry<Identifier, Resource> it : shaders.entrySet()) {
            GlslPreprocessor preprocessor = ShaderManager.createPreprocessor(shaders, it.getKey());

            try (Reader reader = it.getValue().openAsReader()) {
                String source = IOUtils.toString(reader);
                sources.put(it.getKey(), String.join("", preprocessor.process(source)));
            } catch (IOException ex) {
                log.error("Failed to load compute shader source at {}", it.getKey(), ex);
            }
        }

        return new ComputeShaderSource(sources.build());
    }

    @Override
    protected void apply(ComputeShaderSource preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.source = preparations;
        RegisterComputePipelinesEvent event = new RegisterComputePipelinesEvent();
        ModLoader.postEvent(event);
        ALRGpuDeviceExtension deviceExtension = (ALRGpuDeviceExtension) RenderSystem.getDevice();
        for (ALRComputePipeline pipeline : event.getPipelines()) {
            ALRComputeShaderInstanceKey key = new ALRComputeShaderInstanceKey(pipeline.shaderLocation(), pipeline.defines());

            ALRComputeShaderInstance instance = deviceExtension.alrCompileComputeShader(key);
        }

    }

    @Nullable
    public String getSource(Identifier location) {
        return source.source.get(location);
    }

    public record ComputeShaderSource(
        Map<Identifier, String> source
    ) {
    }
}
