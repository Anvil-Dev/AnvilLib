package dev.anvilcraft.lib.v2.cube.mixin.client;

import dev.anvilcraft.lib.v2.cube.client.model.ModelCapture;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleModelWrapper.class)
abstract class SimpleModelWrapperMixin {
    @Inject(method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ResolvedModel;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/renderer/block/model/BlockModelPart;", at = @At("RETURN"))
    private static void anvillib_cube$capture(ModelBaker baker, ResolvedModel model, ModelState state,
                                               CallbackInfoReturnable<BlockModelPart> callback) {
        if (callback.getReturnValue() == baker.missingBlockModelPart()) return;
        ModelCapture.remember(model, state, callback.getReturnValue());
    }
}
