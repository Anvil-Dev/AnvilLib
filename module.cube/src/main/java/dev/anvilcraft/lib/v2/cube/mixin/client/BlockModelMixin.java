package dev.anvilcraft.lib.v2.cube.mixin.client;

import dev.anvilcraft.lib.v2.cube.client.model.ModelCapture;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(BlockModel.class)
abstract class BlockModelMixin {
    @Inject(method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Z)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("RETURN"))
    private void anvillib_cube$capture(ModelBaker baker, BlockModel owner, Function<Material, TextureAtlasSprite> sprites,
                                       ModelState state, boolean guiLight, CallbackInfoReturnable<BakedModel> callback) {
        ModelCapture.remember((BlockModel) (Object) this, state, callback.getReturnValue());
    }
}
