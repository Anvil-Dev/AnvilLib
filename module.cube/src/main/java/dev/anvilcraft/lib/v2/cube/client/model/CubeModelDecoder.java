package dev.anvilcraft.lib.v2.cube.client.model;

import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;

/** 只在模型加载时转换元素，反向 from/to 与普通 cube 一样作为实体体积。 */
public final class CubeModelDecoder {
    private CubeModelDecoder() { }

    public static List<ConvexShape> decode(List<BlockElement> elements, ModelState modelState) {
        if (elements.isEmpty() || elements.size() > SelectionGeometry.MAX_SHAPES) {
            throw new IllegalArgumentException("Unsupported element count");
        }
        List<ConvexShape> result = new ArrayList<>(elements.size());
        Matrix4d model = new Matrix4d().translation(0.5, 0.5, 0.5)
            .mul(new Matrix4d(modelState.transformation().getMatrix())).translate(-0.5, -0.5, -0.5);
        for (BlockElement element : elements) {
            AABB box = new AABB(element.from().x() / 16.0, element.from().y() / 16.0, element.from().z() / 16.0,
                element.to().x() / 16.0, element.to().y() / 16.0, element.to().z() / 16.0);
            Matrix4d transform = new Matrix4d(model);
            BlockElementRotation rotation = element.rotation();
            if (rotation != null) {
                // 使用实际烘焙矩阵，同时支持单轴和欧拉角旋转及重缩放。
                transform.translate(rotation.origin().x(), rotation.origin().y(), rotation.origin().z())
                    .mul(new Matrix4d(rotation.transform()))
                    .translate(-rotation.origin().x(), -rotation.origin().y(), -rotation.origin().z());
            }
            result.add(ConvexShape.box(box).transform(transform));
        }
        return result;
    }
}
