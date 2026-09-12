package dev.anvilcraft.lib.v2.cube.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import org.joml.Vector3f;

/** 实体和方块共用的绘制入口；调用方在双精度下减去相机坐标后再设置位姿。 */
public final class OutlineRenderer {
    private OutlineRenderer() { }

    public static void render(PoseStack poses, VertexConsumer consumer, PackedOutline outline,
                              float red, float green, float blue, float alpha) {
        render(poses, consumer, outline, red, green, blue, alpha, SelectionGeometry.MAX_OUTLINE_SEGMENTS);
    }

    public static int render(PoseStack poses, VertexConsumer consumer, PackedOutline outline,
                             float red, float green, float blue, float alpha, int maximumSegments) {
        return render(poses, consumer, outline, red, green, blue, alpha, maximumSegments, 2.5F);
    }

    public static int render(PoseStack poses, VertexConsumer consumer, PackedOutline outline,
                             float red, float green, float blue, float alpha, int maximumSegments, float lineWidth) {
        PoseStack.Pose pose = poses.last();
        int count = Math.min(outline.segmentCount(), maximumSegments);
        Vector3f direction = new Vector3f();
        for (int line = 0; line < count; line++) {
            // 线方向是切向量，非均匀缩放时不能使用表面法线的逆转置变换。
            pose.pose().transformDirection(direction.set(outline.coordinate(line, 6),
                outline.coordinate(line, 7), outline.coordinate(line, 8))).normalize();
            for (int point = 0; point < 6; point += 3) {
                consumer.addVertex(pose, outline.coordinate(line, point), outline.coordinate(line, point + 1),
                    outline.coordinate(line, point + 2)).setColor(red, green, blue, alpha)
                    .setNormal(direction.x, direction.y, direction.z).setLineWidth(lineWidth);
            }
        }
        return count;
    }
}
