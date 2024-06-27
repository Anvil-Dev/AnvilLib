package dev.anvilcraft.lib.mixin.fabric;

import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(GameTestServer.class)
public class GameTestServerMixin {
    @Redirect(
        method = "create",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Collection;isEmpty()Z"
        )
    )
    private static boolean create(Collection<?> instance) {
        return false;
    }
}
