package dev.anvilcraft.lib.v2.cube.client.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** 临时关联原版模型烘焙结果；发布选择几何后清空，避免长期保留纹理及未烘焙模型。 */
public final class ModelCapture {
    private static final Map<BlockModelPart, Source> SOURCES = new IdentityHashMap<>();
    private ModelCapture() { }
    public static synchronized void remember(ResolvedModel model, ModelState state, BlockModelPart baked) {
        if (!(baked instanceof SimpleModelWrapper)
            || !(model.getTopGeometry() instanceof SimpleUnbakedGeometry geometry) || geometry.elements().isEmpty()) return;
        var root = model.getTopAdditionalProperties().getOptional(NeoForgeModelProperties.TRANSFORM);
        ModelState transformed = root == null ? state : UnbakedElementsHelper.composeRootTransformIntoModelState(state, root);
        SOURCES.put(baked, new Source(geometry.elements(), transformed));
    }
    public static synchronized Map<BlockModelPart, Source> take() {
        Map<BlockModelPart, Source> sources = new IdentityHashMap<>(SOURCES);
        SOURCES.clear();
        return sources;
    }
    public record Source(List<BlockElement> elements, ModelState state) { }
}
