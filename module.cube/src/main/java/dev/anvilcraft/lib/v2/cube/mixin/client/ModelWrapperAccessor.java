package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.DelegateBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DelegateBakedModel.class)
public interface ModelWrapperAccessor {
    @Accessor("parent")
    BakedModel anvillib_cube$original();
}
