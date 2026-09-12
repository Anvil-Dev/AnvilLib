package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleVariant.class)
public interface SingleVariantAccessor {
    @Accessor("model")
    BlockModelPart anvillib_cube$model();
}
