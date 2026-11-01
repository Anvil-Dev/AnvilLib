package dev.anvilcraft.lib.v2.rendering.mixins.blaze3d.gl;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.backend.opengl.GlProgram;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.NamedUniformAccess;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlProgram.class)
public abstract class GlProgramMixin implements NamedUniformAccess {
    @Shadow public abstract int getProgramId();
    @Unique private final Map<String, Integer> anvillib$uniformLocations = new HashMap<>();

    @Inject(method = "setupBindGroupLayouts", at = @At("RETURN"))
    private void anvillib$recordUniformNames(List<BindGroupLayout.UniformDescription> uniforms, CallbackInfo ci) {
        this.anvillib$uniformLocations.clear();
        for (int i = 0; i < uniforms.size(); i++) {
            String backendName = String.format(Locale.ROOT, "_uniform_%02d_%02d", 0, i);
            this.anvillib$uniformLocations.put(uniforms.get(i).name(), GL33C.glGetUniformLocation(this.getProgramId(), backendName));
        }
    }

    @Override
    public int getUniformLocation(String name, ALRGpuDeviceBackendExtension device) {
        return this.anvillib$uniformLocations.getOrDefault(name, INVALID_UNIFORM_LOCATION);
    }
}
