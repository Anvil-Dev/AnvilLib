package dev.anvilcraft.lib.v2.cube.mixin.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.util.random.WeightedEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WeightedBakedModel.class)
public interface WeightedModelAccessor {
    @Accessor("list")
    net.minecraft.util.random.SimpleWeightedRandomList<BakedModel> anvillib_cube$list();

}
