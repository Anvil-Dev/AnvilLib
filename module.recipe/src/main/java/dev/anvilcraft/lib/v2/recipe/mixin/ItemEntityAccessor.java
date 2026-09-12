package dev.anvilcraft.lib.v2.recipe.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
@ApiStatus.Internal
public interface ItemEntityAccessor {
    @Accessor
    void setAge(int age);
}
