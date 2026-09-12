package dev.anvilcraft.lib.v2.cube.mixin.client;

import com.mojang.math.Transformation;
import dev.anvilcraft.lib.v2.cube.client.model.ModelCapture;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockModel.class)
abstract class BlockModelMixin {
    @Shadow @Final private List<BlockElement> elements;

    @Inject(method = "bake", at = @At("RETURN"))
    private void anvillib_cube$capture(TextureSlots textures, ModelBaker baker, ModelState state,
                                       boolean ambientOcclusion, boolean guiLight, ItemTransforms transforms,
                                       ContextMap properties, CallbackInfoReturnable<BakedModel> callback) {
        // 空元素模型由父模型烘焙，避免覆盖父模型已经捕获的几何。
        ModelCapture.remember(this.elements, state,
            properties.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity()), callback.getReturnValue());
    }
}
