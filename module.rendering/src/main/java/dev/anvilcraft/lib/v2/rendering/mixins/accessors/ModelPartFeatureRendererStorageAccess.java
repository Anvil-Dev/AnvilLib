package dev.anvilcraft.lib.v2.rendering.mixins.accessors;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelPartFeatureRenderer.Storage.class)
public interface ModelPartFeatureRendererStorageAccess {

    @Accessor("solidModelPartSubmits")
    Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> alrGetSolidModelPartSubmits();

    @Accessor("translucentModelPartSubmits")
    Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> alrGetTranslucentModelPartSubmits();
}
