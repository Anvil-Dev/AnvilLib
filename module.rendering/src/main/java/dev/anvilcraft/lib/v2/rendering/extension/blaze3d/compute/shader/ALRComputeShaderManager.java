package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.rendering.event.RegisterComputePipelinesEvent;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRComputeCapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModLoader;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads compute shader sources from the resource manager, compiles them into GL compute programs
 * and caches the results per pipeline.
 * <p>
 * Ported from 26.1 (see #P7e): the reload mechanism ({@link SimplePreparableReloadListener}) and the
 * resource scanning logic are preserved; registration happens through
 * {@code RegisterClientReloadListenersEvent}. {@code ShaderManager.createPreprocessor} (26.1) is
 * replaced by an inline {@link GlslPreprocessor} resolving {@code #moj_import} the same way the
 * 1.21.1 core shader pipeline does. The 26.1 {@code GpuDevice}-backed compile/destroy logic is
 * inlined with raw LWJGL calls.
 */
@Slf4j
public class ALRComputeShaderManager extends SimplePreparableReloadListener<ALRComputeShaderManager.ComputeShaderSource> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ALRComputeShaderManager INSTANCE = new ALRComputeShaderManager();

    @UnknownNullability
    private ComputeShaderSource source = null;

    private final Map<ALRComputeProgramInstanceKey, ALRComputeProgramInstance> shaderInstanceMap = new HashMap<>();
    private final Map<ALRComputePipeline, ALRComputeProgramInstance> pipelineToProgramMap = new HashMap<>();

    @Override
    protected @NonNull ComputeShaderSource prepare(ResourceManager manager, ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, String> sources = new ImmutableMap.Builder<>();

        Map<ResourceLocation, Resource> shaders = manager.listResources(
            "shaders",
            it -> it.getPath().endsWith(".csh") || it.getPath().endsWith(".compute") || it.getPath().endsWith(".glsl")
        );

        for (Map.Entry<ResourceLocation, Resource> it : shaders.entrySet()) {
            GlslPreprocessor preprocessor = createPreprocessor(manager, it.getKey());

            try (Reader reader = it.getValue().openAsReader()) {
                String source = IOUtils.toString(reader);
                sources.put(it.getKey(), String.join("\n", preprocessor.process(source)));
            } catch (IOException ex) {
                log.error("Failed to load compute shader source at {}", it.getKey(), ex);
            }
        }
        log.info("Loaded {} compute shader.", shaders.size());
        return new ComputeShaderSource(sources.build());
    }

    @Override
    protected void apply(ComputeShaderSource preparations, ResourceManager manager, ProfilerFiller profiler) {
        ALRComputeCapabilities.init();
        if (!ALRComputeCapabilities.isComputeSupported()) {
            return;
        }
        for (ALRComputeProgramInstance value : shaderInstanceMap.values()) {
            destroyComputeShader(value);
        }
        shaderInstanceMap.clear();
        pipelineToProgramMap.clear();

        this.source = preparations;
        RegisterComputePipelinesEvent<ALRComputePipeline> event = new RegisterComputePipelinesEvent<>();
        ModLoader.postEvent(event);

        for (ALRComputePipeline pipeline : event.getPipelines()) {
            ALRComputeProgramInstanceKey key = new ALRComputeProgramInstanceKey(
                pipeline.shaderLocation(),
                pipeline.defines()
            );
            log.debug("Compiled COMPUTE shader {}", pipeline.shaderLocation());
            ALRComputeProgramInstance instance = compileComputeShader(key);
            this.shaderInstanceMap.put(key, instance);
            this.pipelineToProgramMap.put(pipeline, instance);
        }
    }

    public String getSource(ResourceLocation location) {
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

    private void destroyComputeShader(ALRComputeProgramInstance instance) {
        if (instance.id() != 0) {
            GL20.glDeleteProgram(instance.id());
        }
    }

    /**
     * Compiles a GL compute program from the preprocessed source of the given key.
     * <p>
     * The compile/link sequence is ported line by line from the 26.1 {@code GlDeviceMixin};
     * the debug label application replaces the 26.1 {@code GlDebugLabel} mixins.
     */
    private ALRComputeProgramInstance compileComputeShader(ALRComputeProgramInstanceKey instanceKey) {
        String source = getSource(instanceKey.location());
        if (source == null) {
            LOGGER.error("Could not find COMPUTE shader source for {}", instanceKey.location());
            return ALRComputeProgramInstance.INVALID;
        }
        int shaderId = GL20.glCreateShader(ARBComputeShader.GL_COMPUTE_SHADER);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            String infoLog = GL20.glGetShaderInfoLog(shaderId);
            LOGGER.error("Could not compile COMPUTE shader {}: {}", instanceKey.location(), infoLog);
            GL20.glDeleteShader(shaderId);
            return ALRComputeProgramInstance.INVALID;
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shaderId);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String infoLog = GL20.glGetProgramInfoLog(shaderId);
            LOGGER.error("Could not link COMPUTE shader {}: {}", instanceKey.location(), infoLog);
            GL20.glDeleteShader(shaderId);
            GL20.glDeleteProgram(program);
            return ALRComputeProgramInstance.INVALID;
        }
        GL20.glDeleteShader(shaderId);
        ALRComputeProgramInstance instance = new ALRComputeProgramInstance(program, instanceKey);
        applyDebugLabel(instance);
        return instance;
    }

    private void applyDebugLabel(ALRComputeProgramInstance shaderInstance) {
        if (!GL.getCapabilities().GL_KHR_debug) {
            return;
        }
        KHRDebug.glObjectLabel(
            KHRDebug.GL_PROGRAM,
            shaderInstance.id(),
            shaderInstance.key().location().toString()
        );
    }

    /**
     * A minimal GLSL preprocessor for compute shaders: resolves {@code #moj_import<...>} the same
     * way the 1.21.1 core shader pipeline does ({@code ClientHooks.getShaderImportLocation}).
     */
    private static GlslPreprocessor createPreprocessor(ResourceManager manager, ResourceLocation base) {
        return new GlslPreprocessor() {
            private final Set<String> importedPaths = new HashSet<>();

            @Override
            public String applyImport(boolean useFullPath, String directory) {
                ResourceLocation resourcelocation = net.neoforged.neoforge.client.ClientHooks.getShaderImportLocation(base.toString(), useFullPath, directory);
                if (!this.importedPaths.add(resourcelocation.toString())) {
                    return null;
                } else {
                    try {
                        try (Reader reader = manager.openAsReader(resourcelocation)) {
                            return IOUtils.toString(reader);
                        }
                    } catch (IOException ioexception) {
                        LOGGER.error("Could not open GLSL import {}: {}", resourcelocation, ioexception.getMessage());
                        return "#error " + ioexception.getMessage();
                    }
                }
            }
        };
    }

    public record ComputeShaderSource(
        Map<ResourceLocation, String> source
    ) {
    }
}
