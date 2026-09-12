package dev.anvilcraft.lib.v2.rendering.integration.mixins;

import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OuterWrappedRenderType.class)
public interface OuterWrappedRenderTypeAccess {
    @Accessor("wrapped")
    RenderType alr$unwrap();
}
