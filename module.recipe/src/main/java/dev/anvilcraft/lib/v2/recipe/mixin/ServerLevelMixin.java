package dev.anvilcraft.lib.v2.recipe.mixin;

import dev.anvilcraft.lib.v2.recipe.cache.EntityDropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 容器掉落会直接加入世界，不能仅依赖 Entity.captureDrops；提交后才触发真正的入世界事件。 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void anvillib$captureRecipeDrops(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (EntityDropCapture.capture(entity)) cir.setReturnValue(false);
    }
}
