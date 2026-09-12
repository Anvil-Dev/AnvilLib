package dev.anvilcraft.lib.v2.cube.mixin.client;

import dev.anvilcraft.lib.v2.cube.client.CubePicking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Redirect(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult anvillib_cube$pick(Entity entity, double range, float partialTick, boolean fluids) {
        return CubePicking.pick(entity, range, partialTick, fluids);
    }
}
