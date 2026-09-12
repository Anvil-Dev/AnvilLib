package dev.anvilcraft.lib.v2.rendering.mixins.accessors;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelFeatureRenderer.Storage.class)
public interface ModelFeatureRendererStorageAccess {

    @Accessor("solidModelSubmits")
    Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> alrGetSolidModelSubmits();

    @Accessor("translucentModelSubmits")
    List<SubmitNodeStorage.TranslucentModelSubmit<?>> alrGetTranslucentModelSubmits();
}
