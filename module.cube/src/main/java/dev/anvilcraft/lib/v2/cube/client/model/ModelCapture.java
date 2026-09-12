package dev.anvilcraft.lib.v2.cube.client.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import com.mojang.math.Transformation;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** 临时关联原版模型烘焙结果；发布选择几何后清空，避免长期保留纹理及未烘焙模型。 */
public final class ModelCapture {
    private static final Map<BakedModel, Source> SOURCES = new IdentityHashMap<>();
    private ModelCapture() { }

    public static synchronized void remember(List<BlockElement> elements, ModelState state, Transformation root, BakedModel baked) {
        if (elements.isEmpty()) return;
        ModelState transformed = root.isIdentity() ? state
            : UnbakedElementsHelper.composeRootTransformIntoModelState(state, root);
        SOURCES.put(baked, new Source(List.copyOf(elements), transformed));
    }

    public static synchronized Map<BakedModel, Source> take() {
        Map<BakedModel, Source> sources = new IdentityHashMap<>(SOURCES);
        SOURCES.clear();
        return sources;
    }

    public record Source(List<BlockElement> elements, ModelState state) { }
}
