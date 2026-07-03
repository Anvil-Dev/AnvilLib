package dev.anvilcraft.lib.v2.piston.injection;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

public interface IPistonMovingBlockEntityExtension {
    @ApiStatus.Internal
    default @Nullable BlockEntity anvillib$clearBlockEntity() {
        throw new AssertionError("No Implemented!");
    }

    @ApiStatus.Internal
    default @Nullable BlockEntity anvillib$getBlockEntity() {
        throw new AssertionError("No Implemented!");
    }

    @ApiStatus.Internal
    default void anvillib$setBlockEntity(@Nullable BlockEntity blockEntity) {
        throw new AssertionError("No Implemented!");
    }
}
