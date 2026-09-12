package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDebugLabel;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.ALROptions;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ExtendedTextureFormat;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRDebugLabelExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstanceKey;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeShaderManager;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.gl.GlSamplesQuery;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.bindless.BindlessTexturingSupport;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.GlExtendedTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.GlExtendedTextureConstants;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.texture.gl.bindless.GlBindlessTexturingSupport;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public abstract class GlDeviceMixin implements ALRGpuDeviceBackendExtension {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract GlDebugLabel debugLabels();

    @Shadow
    @Final
    private GlDebugLabel debugLabels;

    @Unique
    private ALRHICapabilities alr$capabilities = null;
    @Unique
    private BindlessTexturingSupport alr$bindlessTexturingImpl;

    @Override
    public void alrDestroyComputeShader(ALRComputeProgramInstance instance) {
        GL46.glDeleteProgram(instance.id());
    }

    @Override
    public ALRComputeProgramInstance alrCompileComputePipeline(
        ALRComputePipeline pipeline,
        ALRComputeProgramInstanceKey instanceKey
    ) {
        String source = ALRComputeShaderManager.INSTANCE.getSource(instanceKey.location());
        int shaderId = GL46.glCreateShader(ARBComputeShader.GL_COMPUTE_SHADER);
        GL46.glShaderSource(shaderId, GlslPreprocessor.injectDefines(source, instanceKey.defines()));
        GL46.glCompileShader(shaderId);
        if (GL46.glGetShaderi(shaderId, GL46.GL_COMPILE_STATUS) == 0) {
            String infoLog = GL46.glGetShaderInfoLog(shaderId);
            GL46.glDeleteShader(shaderId);
            LOGGER.error("Could not compile COMPUTE shader {}: {}", instanceKey.location(), infoLog);
            return ALRComputeProgramInstance.INVALID;
        }
        int program = GL46.glCreateProgram();
        GL46.glAttachShader(program, shaderId);
        GL46.glLinkProgram(program);
        if (GL46.glGetProgrami(program, GL46.GL_LINK_STATUS) == 0) {
            String infoLog = GL46.glGetProgramInfoLog(program);
            LOGGER.error("Could not link COMPUTE shader {}: {}", instanceKey.location(), infoLog);
            GL46.glDeleteShader(shaderId);
            GL46.glDeleteProgram(program);
            return ALRComputeProgramInstance.INVALID;
        }
        GL46.glDeleteShader(shaderId);
        ALRComputeProgramInstance instance = new ALRComputeProgramInstance(program, instanceKey, pipeline);
        ((ALRDebugLabelExtension) this.debugLabels()).alrApplyLabel(instance);
        return instance;
    }

    @Override
    public void alrPushDebugGroup(Supplier<String> message) {
        this.debugLabels().pushDebugGroup(message);
    }

    @Override
    public void alrPopDebugGroup() {
        this.debugLabels().popDebugGroup();
    }

    @Override
    public GpuQueryObject alrCreateSamplesQuery() {
        return new GlSamplesQuery();
    }

    @Override
    public ALRHICapabilities alrhiCreateCapabilities() {
        if (this.alr$capabilities == null) {
            GLCapabilities capabilities = GL.getCapabilities();
            this.alr$capabilities = new ALRHICapabilities(
                capabilities.GL_ARB_compute_shader,
                capabilities.GL_ARB_bindless_texture,
                capabilities.GL_ARB_buffer_storage,
                capabilities.GL_KHR_shader_subgroup,
                GL46.glGetInteger(GL46.GL_MAX_IMAGE_UNITS)
            );
        }
        return alr$capabilities;
    }

    @Override
    public GpuTexture alrCreateExtendedTexture(
        @Nullable String label,
        int usage,
        ExtendedTextureFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ) {
        GlStateManager.clearGlErrors();
        int id = GlStateManager._genTexture();
        if (label == null) {
            label = String.valueOf(id);
        }

        boolean isCubemap = (usage & 16) != 0;
        int target;
        if (isCubemap) {
            GL11.glBindTexture(34067, id);
            target = 34067;
        } else {
            GlStateManager._bindTexture(id);
            target = 3553;
        }

        GlStateManager._texParameter(target, 33085, mipLevels - 1);
        GlStateManager._texParameter(target, 33082, 0);
        GlStateManager._texParameter(target, 33083, mipLevels - 1);

        if (isCubemap) {
            for (int cubeTarget : GlConst.CUBEMAP_TARGETS) {
                for (int i = 0; i < mipLevels; i++) {
                    GlStateManager._texImage2D(
                        cubeTarget,
                        i,
                        GlExtendedTextureConstants.toGlInternalId(format),
                        width >> i,
                        height >> i,
                        0,
                        GlExtendedTextureConstants.toGlExternalId(format),
                        GlExtendedTextureConstants.toGlType(format),
                        null
                    );
                }
            }
        } else {
            for (int i = 0; i < mipLevels; i++) {
                GlStateManager._texImage2D(
                    target,
                    i,
                    GlExtendedTextureConstants.toGlInternalId(format),
                    width >> i,
                    height >> i,
                    0,
                    GlExtendedTextureConstants.toGlExternalId(format),
                    GlExtendedTextureConstants.toGlType(format),
                    null
                );
            }
        }

        int error = GlStateManager._getError();
        if (error == 1285) {
            throw new GpuOutOfMemoryException("Could not allocate texture of " + width + "x" + height + " for " + label);
        } else if (error != 0) {
            throw new IllegalStateException("OpenGL error " + error);
        } else {
            GlExtendedTexture texture = new GlExtendedTexture(
                usage,
                label,
                format,
                width,
                height,
                depthOrLayers,
                mipLevels,
                id
            );
            this.debugLabels.applyLabel(texture);
            return texture;
        }
    }

    @Override
    public int alrGetUniformLocation(ALRComputeProgramInstance program, ALRComputePipeline owner, String name) {
        return switch (owner.getBindingType(name)) {
            case TEXTURE_OR_IMAGE -> GlStateManager._glGetUniformLocation(program.id(), name);
            case null -> -1;
            default -> throw new IllegalStateException(
                "Shader resource other than texture or image is not allowed to getLocation");
        };
    }

    @Override
    public BindlessTexturingSupport alrGetBindlessTexturingSupport() {
        if (alrhiCreateCapabilities().bindlessTexturing() && alr$bindlessTexturingImpl == null) {
            this.alr$bindlessTexturingImpl = new GlBindlessTexturingSupport(this);
        }
        return this.alr$bindlessTexturingImpl;
    }
}
