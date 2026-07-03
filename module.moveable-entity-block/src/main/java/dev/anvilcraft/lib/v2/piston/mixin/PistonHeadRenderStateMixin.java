package dev.anvilcraft.lib.v2.piston.mixin;

import dev.anvilcraft.lib.v2.piston.injection.IPistonHeadRenderStateExtension;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PistonHeadRenderState.class)
abstract class PistonHeadRenderStateMixin implements IPistonHeadRenderStateExtension {
    @Unique
    private @Nullable BlockEntityRenderState anvillib$extraState = null;

    public void anvillib$setExtraState(@Nullable BlockEntityRenderState anvillib$extraState) {
        this.anvillib$extraState = anvillib$extraState;
    }

    @Override
    public @Nullable BlockEntityRenderState anvillib$getExtraState() {
        return this.anvillib$extraState;
    }
}
