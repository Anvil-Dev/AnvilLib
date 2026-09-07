package dev.anvilcraft.lib.v2.cube.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4d;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.List;

/** 几何可跨帧复用，矩阵将局部坐标变换到方块坐标；平移不进入轮廓缓存键。 */
public final class SelectionPart {
    private final SelectionGeometry geometry;
    private final @Nullable Matrix4f transform;
    private final @Nullable Matrix4f inverse;
    private final AABB bounds;

    public SelectionPart(SelectionGeometry geometry) {
        this.geometry = Objects.requireNonNull(geometry);
        this.transform = null;
        this.inverse = null;
        this.bounds = geometry.bounds();
    }

    public SelectionPart(SelectionGeometry geometry, Matrix4fc localToBlock) {
        this.geometry = Objects.requireNonNull(geometry);
        if (!localToBlock.isFinite() || !localToBlock.isAffine() || Math.abs(localToBlock.determinant()) < 1.0E-9) {
            throw new IllegalArgumentException("Expected an invertible affine transform");
        }
        this.transform = new Matrix4f(localToBlock);
        this.inverse = this.transform.invert(new Matrix4f());
        AABB local = geometry.bounds();
        AABB result = null;
        for (int i = 0; i < 8; i++) {
            Vec3 vertex = position(this.transform, new Vec3((i & 1) == 0 ? local.minX : local.maxX,
                (i & 2) == 0 ? local.minY : local.maxY, (i & 4) == 0 ? local.minZ : local.maxZ));
            AABB point = new AABB(vertex, vertex);
            result = result == null ? point : result.minmax(point);
        }
        this.bounds = Objects.requireNonNull(result);
    }

    public SelectionGeometry geometry() { return this.geometry; }
    public AABB bounds() { return this.bounds; }

    /** 供资源烘焙阶段组合 multipart 使用，动画每帧只应复用几何并更新矩阵。 */
    public List<ConvexShape> blockSpaceShapes() {
        if (this.transform == null) return this.geometry.shapes();
        Matrix4d matrix = new Matrix4d(this.transform);
        return this.geometry.shapes().stream().map(shape -> shape.transform(matrix)).toList();
    }

    public void apply(PoseStack stack) {
        if (this.transform != null) stack.mulPose(this.transform);
    }

    public @Nullable SelectionGeometry.RayHit clip(Vec3 start, Vec3 end) {
        if (this.inverse == null) return this.geometry.clip(start, end);
        SelectionGeometry.RayHit hit = this.geometry.clip(position(this.inverse, start), position(this.inverse, end));
        if (hit == null) return null;
        Vec3 n = hit.normal();
        // 法线使用逆转置，保证非均匀缩放和镜像变换下的交互面方向正确。
        Vec3 normal = new Vec3(
            this.inverse.m00() * n.x + this.inverse.m01() * n.y + this.inverse.m02() * n.z,
            this.inverse.m10() * n.x + this.inverse.m11() * n.y + this.inverse.m12() * n.z,
            this.inverse.m20() * n.x + this.inverse.m21() * n.y + this.inverse.m22() * n.z
        ).normalize();
        return new SelectionGeometry.RayHit(hit.fraction(), normal, hit.inside());
    }

    private static Vec3 position(Matrix4fc matrix, Vec3 point) {
        Vector3f result = matrix.transformPosition((float) point.x, (float) point.y, (float) point.z, new Vector3f());
        return new Vec3(result.x, result.y, result.z);
    }
}
