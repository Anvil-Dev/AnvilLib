package dev.anvilcraft.lib.v2.rendering.mixins.accessors;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(NameTagFeatureRenderer.Storage.class)
public interface NameTagFeatureRendererStorageAccess {

    @Accessor("nameTagSubmitsSeethrough")
    List<SubmitNodeStorage.NameTagSubmit> alrGetNameTagSubmitsSeethrough();

    @Accessor("nameTagSubmitsNormal")
    List<SubmitNodeStorage.NameTagSubmit> alrGetNameTagSubmitsNormal();
}
