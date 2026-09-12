package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleVariant.class)
public interface SingleVariantAccessor {
    @Accessor("model")
    BlockStateModelPart anvillib_cube$model();
}
