package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedModelWrapper.class)
public interface ModelWrapperAccessor {
    @Accessor("originalModel")
    BakedModel anvillib_cube$original();
}
