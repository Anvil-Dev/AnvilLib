package dev.anvilcraft.lib.v2.piston.injection;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

public interface IPistonHeadRenderStateExtension {
    @ApiStatus.Internal
    default void anvillib$setExtraState(@Nullable BlockEntityRenderState state) {
        throw new AssertionError("No Implemented!");
    }

    @ApiStatus.Internal
    default @Nullable BlockEntityRenderState anvillib$getExtraState() {
        throw new AssertionError("No Implemented!");
    }
}
