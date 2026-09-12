package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.renderer.block.model.multipart.MultiPartModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPartModel.class)
public interface MultipartModelAccessor {
    @Accessor("shared")
    MultiPartModel.SharedBakedState anvillib_cube$shared();
}
