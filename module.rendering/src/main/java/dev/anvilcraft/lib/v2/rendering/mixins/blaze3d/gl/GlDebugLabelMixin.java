package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.blaze3d.opengl.GlDebugLabel;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRDebugLabelExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader.ALRComputeProgramInstance;
import net.minecraft.util.StringUtil;
import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.KHRDebug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@org.jetbrains.annotations.ApiStatus.Internal
public class GlDebugLabelMixin {
    @Mixin(GlDebugLabel.class)
    public static class Self implements ALRDebugLabelExtension {

        @Override
        public void alrApplyLabel(ALRComputeProgramInstance shaderInstance) {
        }
    }

    @Mixin(targets = "com.mojang.blaze3d.opengl.GlDebugLabel$Core")
    public static class Core implements ALRDebugLabelExtension {

        @Shadow
        @Final
        private int maxLabelLength;

        @Override
        public void alrApplyLabel(ALRComputeProgramInstance shaderInstance) {
            KHRDebug.glObjectLabel(
                GL46.GL_PROGRAM,
                shaderInstance.id(),
                StringUtil.truncateStringIfNecessary(
                    shaderInstance.key().location().toString(),
                    this.maxLabelLength,
                    true
                )
            );
        }
    }

    @Mixin(targets = "com.mojang.blaze3d.opengl.GlDebugLabel$Ext")
    public static class Ext implements ALRDebugLabelExtension {

        @Override
        public void alrApplyLabel(ALRComputeProgramInstance shaderInstance) {
            EXTDebugLabel.glLabelObjectEXT(
                EXTDebugLabel.GL_SHADER_OBJECT_EXT,
                shaderInstance.id(),
                StringUtil.truncateStringIfNecessary(
                    shaderInstance.key().location().toString(),
                    256,
                    true
                )
            );
        }
    }
}
