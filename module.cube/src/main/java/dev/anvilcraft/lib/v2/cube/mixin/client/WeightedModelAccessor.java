package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WeightedVariants.class)
public interface WeightedModelAccessor {
    @Accessor("list")
    net.minecraft.util.random.WeightedList<BlockStateModel> anvillib_cube$variants();
}
