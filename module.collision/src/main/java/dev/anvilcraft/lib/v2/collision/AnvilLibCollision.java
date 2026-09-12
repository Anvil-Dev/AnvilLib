package dev.anvilcraft.lib.v2.collision;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public class AnvilLibCollision {
    /**
     * 沿三轴独立扫掠：AABB 分别沿 X、Y、Z 轴运动，逐轴计算最大安全位移。
     * <p>
     * 每个轴的处理是独立的——沿 X 扫掠时 Y、Z 坐标保持不变，沿 Y 扫掠时 X、Z
     * 不变。因此当对角方向有障碍物时可能偏保守。
     * <p>
     * 三轴独立的好处是与 Minecraft 原版碰撞一致：各分量被各自的障碍物"推开"，
     * 避免了沿斜面滑动的问题。
     *
     * @param min       AABB 最小角（世界坐标）
     * @param max       AABB 最大角（世界坐标）
     * @param motion    各轴最大位移（分量可正可负）
     * @param triangles 三角形数组，每 3 个顶点构成一个三角形
     * @param epsilon   碰撞容差——间距小于此值即视为碰撞
     * @return 实际可移动位移，各分量符号与 {@code motion} 一致，
     *         绝对值 ≤ {@code |motion|}。已重叠 → 0，无碰撞 → 原始分量
     */
    public static Vector3dc intersectsAABBTriangle(Vector3dc min, Vector3dc max, Vector3dc motion, Vector3fc[] triangles, double epsilon) {
        Vector3d center = min.add(max, new Vector3d()).mul(0.5);
        Vector3d halfExtents = max.sub(min, new Vector3d()).mul(0.5);
        Vector3d result = new Vector3d();

        for (int axisIndex = 0; axisIndex < 3; axisIndex++) {
            double d = motion.get(axisIndex);
            if (Math.abs(d) < 1e-15) {
                result.setComponent(axisIndex, 0.0);
                continue;
            }

            Vector3d axisVec = new Vector3d();
            axisVec.setComponent(axisIndex, 1.0);

            double safeDist = d;

            int triCount = triangles.length / 3;
            for (int i = 0; i < triCount; i++) {
                Vector3fc V0 = triangles[i * 3];
                Vector3fc V1 = triangles[i * 3 + 1];
                Vector3fc V2 = triangles[i * 3 + 2];

                double tHit = collisionDistanceOnAxis(V0, V1, V2, center, halfExtents, axisVec, d, epsilon);

                if (d > 0) {
                    if (tHit < safeDist && tHit >= 0) {
                        safeDist = tHit;
                    }
                } else {
                    if (tHit > safeDist && tHit <= 0) {
                        safeDist = tHit;
                    }
                }

                if ((d > 0 && safeDist <= 0) || (d < 0 && safeDist >= 0)) {
                    break;
                }
            }
            result.setComponent(axisIndex, safeDist);
        }
        return result;
    }

    /**
     * 真扫掠碰撞：AABB 沿 {@code motion} 方向平移，求首次碰到任意三角形时的位移。
     * <p>
     * 与 {@link #intersectsAABBTriangle(Vector3dc, Vector3dc, Vector3dc, Vector3fc[], double)}
     * 的三轴独立不同，此方法沿 motion 的合成方向扫掠，适用于需要精确斜角碰撞判定的场景。
     * <p>
     * 返回值为 {@code motion * t}，其中 t ∈ [0,1] 为安全比例：
     * <ul>
     *   <li>t = 1 → 全程无碰撞</li>
     *   <li>t = 0 → 起始位置已重叠</li>
     *   <li>{@code 0 < t < 1} → 碰撞发生在运动途中</li>
     * </ul>
     * <p>
     * 对每个三角形使用 SAT（分离轴定理）计算碰撞时间区间，取所有三角形中的最小值。
     *
     * @param min       AABB 最小角（世界坐标）
     * @param max       AABB 最大角（世界坐标）
     * @param motion    位移向量（方向 + 大小）
     * @param triangles 三角形数组，每 3 个顶点构成一个三角形
     * @param epsilon   碰撞容差——间距小于此值即视为碰撞
     * @return 沿 {@code motion} 方向的最大安全位移向量。
     *         无碰撞时等于 {@code motion}，初始重叠时为零向量
     */
    public static Vector3dc sweptCollisionAABBTriangle(Vector3dc min, Vector3dc max, Vector3dc motion, Vector3fc[] triangles, double epsilon) {
        double dist = motion.length();
        if (dist < 1e-15) {
            return new Vector3d();
        }

        Vector3d dir = motion.div(dist, new Vector3d());
        Vector3d center = min.add(max, new Vector3d()).mul(0.5);
        Vector3d halfExtents = max.sub(min, new Vector3d()).mul(0.5);

        double minT = dist;
        int triCount = triangles.length / 3;
        for (int i = 0; i < triCount; i++) {
            Vector3fc V0 = triangles[i * 3];
            Vector3fc V1 = triangles[i * 3 + 1];
            Vector3fc V2 = triangles[i * 3 + 2];

            double tHit = collisionDistanceOnAxis(V0, V1, V2, center, halfExtents, dir, dist, epsilon);
            if (tHit < minT) {
                minT = tHit;
                if (minT <= 0) break;
            }
        }

        return dir.mul(minT, new Vector3d());
    }

    /**
     * 计算 AABB 沿 {@code axisVec} 方向运动时，与单个三角形的首次碰撞位移。
     * <p>
     * 三角形顶点被平移到以 {@code center} 为原点的局部坐标系，
     * 使用 SAT（分离轴定理）扫描 13 条轴（3 坐标轴 + 1 面法线 + 9 边叉积轴），
     * 由 {@link #updateInterval} 逐轴收窄碰撞时间区间，最后取与运动方向一致的边界。
     *
     * @param V0,V1,V2   三角形顶点（世界坐标）
     * @param center     AABB 中心点
     * @param halfExtents AABB 半边长
     * @param axisVec    运动方向（无需单位化）
     * @param d          最大位移量（沿 {@code axisVec} 方向，可正可负）
     * @param epsilon    碰撞容差
     * @return 首次碰撞位移（有符号）。0 = 初始已重叠，d = 全程无碰撞
     */
    private static double collisionDistanceOnAxis(
        Vector3fc V0,
        Vector3fc V1,
        Vector3fc V2,
        Vector3dc center,
        Vector3dc halfExtents,
        Vector3dc axisVec,
        double d,
        double epsilon
    ) {

        // 将三角形平移到 AABB 中心为原点的局部坐标
        Vector3d v0 = new Vector3d(V0).sub(center);
        Vector3d v1 = new Vector3d(V1).sub(center);
        Vector3d v2 = new Vector3d(V2).sub(center);

        // 初始就已经碰撞 → 安全距离为 0
        if (initialOverlap(v0, v1, v2, halfExtents, epsilon)) {
            return 0.0;
        }

        // 三角形边向量
        Vector3d f0 = v1.sub(v0, new Vector3d());
        Vector3d f1 = v2.sub(v1, new Vector3d());
        Vector3d f2 = v0.sub(v2, new Vector3d());

        // 面法线
        Vector3d normal = f0.cross(f1, new Vector3d());
        boolean hasNormal = normal.lengthSquared() > 1e-15;
        if (hasNormal) normal.normalize();

        // AABB 三个面法线
        Vector3d[] aabbAxes = {
            new Vector3d(1, 0, 0),
            new Vector3d(0, 1, 0),
            new Vector3d(0, 0, 1)
        };
        Vector3d[] triEdges = {
            f0,
            f1,
            f2
        };

        // 交集区间 [tLow, tHigh] 表示所有分离轴都重叠的 t 范围
        double[] interval = {
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY
        };

        // 测试 3 个坐标轴
        for (Vector3d axis : aabbAxes) {
            if (!updateInterval(axis, v0, v1, v2, halfExtents, axisVec, epsilon, interval)) {
                return d; // 无碰撞
            }
        }

        // 测试三角形面法线
        if (hasNormal) {
            if (!updateInterval(normal, v0, v1, v2, halfExtents, axisVec, epsilon, interval)) {
                return d;
            }
        }

        // 测试 9 条边叉积轴
        for (Vector3d edge : triEdges) {
            for (Vector3d aabbAxis : aabbAxes) {
                Vector3d axis = edge.cross(aabbAxis, new Vector3d());
                if (axis.lengthSquared() > 1e-15) {
                    axis.normalize();
                    if (!updateInterval(axis, v0, v1, v2, halfExtents, axisVec, epsilon, interval)) {
                        return d;
                    }
                }
            }
        }

        double tLow = interval[0];
        double tHigh = interval[1];

        // 若交集为空或退化，则无碰撞
        if (tLow > tHigh + 1e-12) {
            return d;
        }

        // 根据运动方向取第一个（最接近 0 的）碰撞位移
        if (d > 0) {
            if (tHigh < 0) return d;   // 区间全在负半轴
            if (tLow > 0) return tLow;
            return d;
        } else {
            if (tLow > 0) return d;    // 区间全在正半轴
            if (tHigh < 0) return tHigh;
            return d;
        }
    }

    /**
     * 在给定 SAT 分离轴上计算 AABB 与三角形重叠的 t 区间，并与当前区间求交。
     * <p>
     * AABB 中心沿 {@code axisVec} 运动，在参数 t 时刻的位置为 {@code center + t * axisVec}。
     * 三角形在轴上的投影为 [tMin, tMax]，AABB 的投影半径在运动中不变（仅平移，不旋转）。
     * <p>
     * 若该轴与运动方向垂直（{@code dot ≈ 0}），则检查静态分离；否则求解线性不等式
     * 得到重叠的 t 范围 [low, high]，与当前区间取交集。
     *
     * @param axis        SAT 分离轴（无需单位化）
     * @param v0,v1,v2    局部坐标系下的三角形顶点
     * @param halfExtents AABB 半边长
     * @param axisVec     运动方向
     * @param epsilon     碰撞容差
     * @param interval    [0]=tLow, [1]=tHigh，会被就地更新
     * @return false 表示该轴永久分离，运动全程不可能碰撞
     */
    private static boolean updateInterval(
        Vector3d axis,
        Vector3d v0,
        Vector3d v1,
        Vector3d v2,
        Vector3dc halfExtents,
        Vector3dc axisVec,
        double epsilon,
        double[] interval
    ) {

        // 三角形投影
        double p0 = axis.dot(v0);
        double p1 = axis.dot(v1);
        double p2 = axis.dot(v2);
        double tMin = Math.min(Math.min(p0, p1), p2);
        double tMax = Math.max(Math.max(p0, p1), p2);

        // AABB 投影半径
        double r = Math.abs(axis.x()) * halfExtents.x() + Math.abs(axis.y()) * halfExtents.y() + Math.abs(axis.z()) * halfExtents.z();

        double dot = axis.dot(axisVec); // 移动方向在分离轴上的投影

        if (Math.abs(dot) < 1e-15) {
            // 轴与运动方向垂直 → 投影不随 t 改变
            // 若此时该轴是分离轴，则运动全程分离 → 无碰撞可能
            if (-r > tMax + epsilon || tMin - epsilon > r) {
                return false; // 永远分离
            }
            // 否则该轴始终重叠，不提供约束
            return true;
        }

        double low, high;
        if (dot > 0) {
            low = (tMin - epsilon - r) / dot;
            high = (tMax + epsilon + r) / dot;
        } else {
            low = (tMax + epsilon + r) / dot; // dot 为负，low 是较小值
            high = (tMin - epsilon - r) / dot;
        }

        interval[0] = Math.max(interval[0], low);
        interval[1] = Math.min(interval[1], high);

        return interval[0] <= interval[1] + 1e-12;
    }

    /**
     * 静态 SAT 检查：AABB 与三角形在初始位置（t=0，局部坐标）是否重叠。
     * <p>
     * 测试全部 13 条分离轴（3 坐标轴 + 1 面法线 + 9 边叉积轴），
     * 若任意轴分离则返回 false。
     *
     * @param v0,v1,v2    局部坐标系下的三角形顶点（已减去 AABB 中心）
     * @param halfExtents AABB 半边长
     * @param epsilon     碰撞容差
     * @return true 表示重叠（或 gap ≤ epsilon）
     */
    private static boolean initialOverlap(Vector3d v0, Vector3d v1, Vector3d v2, Vector3dc halfExtents, double epsilon) {

        // 三个坐标轴
        if (!overlapOnAxis(new Vector3d(1, 0, 0), v0, v1, v2, halfExtents, epsilon)) return false;
        if (!overlapOnAxis(new Vector3d(0, 1, 0), v0, v1, v2, halfExtents, epsilon)) return false;
        if (!overlapOnAxis(new Vector3d(0, 0, 1), v0, v1, v2, halfExtents, epsilon)) return false;

        Vector3d f0 = v1.sub(v0, new Vector3d());
        Vector3d f1 = v2.sub(v1, new Vector3d());
        Vector3d normal = f0.cross(f1, new Vector3d());
        if (normal.lengthSquared() > 1e-15) {
            normal.normalize();
            if (!overlapOnAxis(normal, v0, v1, v2, halfExtents, epsilon)) return false;
        }

        Vector3d f2 = v0.sub(v2, new Vector3d());
        Vector3d[] edges = {
            f0,
            f1,
            f2
        };
        Vector3d[] axes = {
            new Vector3d(1, 0, 0),
            new Vector3d(0, 1, 0),
            new Vector3d(0, 0, 1)
        };
        for (Vector3d edge : edges) {
            for (Vector3d a : axes) {
                Vector3d axis = edge.cross(a, new Vector3d());
                if (axis.lengthSquared() > 1e-15) {
                    axis.normalize();
                    if (!overlapOnAxis(axis, v0, v1, v2, halfExtents, epsilon)) return false;
                }
            }
        }
        return true; // 所有轴都重叠 → 碰撞
    }

    /**
     * 静态碰撞检测：判断 AABB 与任意三角形是否重叠。
     * <p>
     * 对每个三角形先做包围盒粗筛（AABB-AABB 快速剔除），
     * 再以完整的 SAT（13 条分离轴）进行精确判定。
     *
     * @param min       AABB 最小角
     * @param max       AABB 最大角
     * @param triangles 三角形数组，每 3 个顶点构成一个三角形
     * @param epsilon   碰撞容差——间距小于此值即视为重叠
     * @return true 表示存在至少一个三角形与 AABB 碰撞
     */
    public static boolean intersectsAABBTriangle(Vector3dc min, Vector3dc max, Vector3fc[] triangles, double epsilon) {
        Vector3dc center = min.add(max, new Vector3d()).mul(0.5);
        Vector3dc halfExtents = max.sub(min, new Vector3d()).mul(0.5);

        int triangleCount = triangles.length / 3;
        for (int i = 0; i < triangleCount; i++) {
            Vector3fc V0 = triangles[i * 3];
            Vector3fc V1 = triangles[i * 3 + 1];
            Vector3fc V2 = triangles[i * 3 + 2];

            if (triangleIntersectsAABB(V0, V1, V2, center, halfExtents, epsilon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对单个三角形执行完整 SAT 碰撞检测（13 条轴）。
     * <p>
     * 先做三角形包围盒粗筛（AABB-AABB），再将顶点平移到以 AABB 中心为原点的
     * 局部坐标系，依次测试 3 坐标轴 → 面法线 → 9 条边叉积轴。
     *
     * @param V0,V1,V2   三角形顶点（世界坐标）
     * @param center     AABB 中心点
     * @param halfExtents AABB 半边长
     * @param epsilon    碰撞容差
     * @return true 表示三角形与 AABB 重叠（或 gap ≤ epsilon）
     */
    private static boolean triangleIntersectsAABB(
        Vector3fc V0,
        Vector3fc V1,
        Vector3fc V2,
        Vector3dc center,
        Vector3dc halfExtents,
        double epsilon
    ) {

        // 1. 三角形包围盒粗筛（快速剔除）
        double triMinX = Math.min(Math.min(V0.x(), V1.x()), V2.x());
        double triMinY = Math.min(Math.min(V0.y(), V1.y()), V2.y());
        double triMinZ = Math.min(Math.min(V0.z(), V1.z()), V2.z());
        double triMaxX = Math.max(Math.max(V0.x(), V1.x()), V2.x());
        double triMaxY = Math.max(Math.max(V0.y(), V1.y()), V2.y());
        double triMaxZ = Math.max(Math.max(V0.z(), V1.z()), V2.z());

        double boxMinX = center.x() - halfExtents.x();
        double boxMinY = center.y() - halfExtents.y();
        double boxMinZ = center.z() - halfExtents.z();
        double boxMaxX = center.x() + halfExtents.x();
        double boxMaxY = center.y() + halfExtents.y();
        double boxMaxZ = center.z() + halfExtents.z();

        if (triMinX > boxMaxX + epsilon || triMaxX < boxMinX - epsilon || triMinY > boxMaxY + epsilon || triMaxY < boxMinY - epsilon || triMinZ > boxMaxZ + epsilon || triMaxZ < boxMinZ - epsilon) {
            return false;
        }

        // 2. 将三角形顶点平移到 AABB 中心为原点的局部坐标（使用 double 保证精度）
        Vector3d v0 = new Vector3d(V0).sub(center);
        Vector3d v1 = new Vector3d(V1).sub(center);
        Vector3d v2 = new Vector3d(V2).sub(center);

        // 3. 三角形边向量
        Vector3d f0 = v1.sub(v0, new Vector3d());
        Vector3d f1 = v2.sub(v1, new Vector3d());
        Vector3d f2 = v0.sub(v2, new Vector3d());

        Vector3d aabbAxisX = new Vector3d(1, 0, 0);
        Vector3d aabbAxisY = new Vector3d(0, 1, 0);
        Vector3d aabbAxisZ = new Vector3d(0, 0, 1);

        // ---- 测试三个坐标轴 ----
        if (!overlapOnAxis(aabbAxisX, v0, v1, v2, halfExtents, epsilon)) return false;
        if (!overlapOnAxis(aabbAxisY, v0, v1, v2, halfExtents, epsilon)) return false;
        if (!overlapOnAxis(aabbAxisZ, v0, v1, v2, halfExtents, epsilon)) return false;

        // ---- 测试三角形面法线轴 ----
        Vector3d normal = f0.cross(f1, new Vector3d());
        if (normal.lengthSquared() > 1e-15) {
            normal.normalize();
            if (!overlapOnAxis(normal, v0, v1, v2, halfExtents, epsilon)) return false;
        }
        // 如果面积极小（退化为线段或点），跳过此轴（后续边叉积轴可能仍有效）

        // ---- 测试 9 条边叉积轴 ----
        Vector3d[] aabbAxes = {
            aabbAxisX,
            aabbAxisY,
            aabbAxisZ
        };
        Vector3d[] triEdges = {
            f0,
            f1,
            f2
        };

        for (Vector3d edge : triEdges) {
            for (Vector3d aabbAxis : aabbAxes) {
                Vector3d axis = edge.cross(aabbAxis, new Vector3d());
                if (axis.lengthSquared() > 1e-15) {
                    axis.normalize();
                    if (!overlapOnAxis(axis, v0, v1, v2, halfExtents, epsilon)) {
                        return false;
                    }
                }
                // 平行或退化轴忽略，不会提供有效分离
            }
        }

        // 所有轴都重叠 ⇒ 碰撞
        return true;
    }

    /**
     * 测试三角形与中心在原点的 AABB 在给定轴上的投影是否重叠。
     * <p>
     * 三角形投影区间 [tMin, tMax]，AABB 投影区间 [−r, r]，其中
     * r = Σ|axis[i] · halfExtents[i]|。若两区间带容差后仍分离则返回 false。
     * <p>
     * 分离条件：tMin > r + epsilon 或 −r > tMax + epsilon。
     *
     * @param axis          投影轴（无需单位化）
     * @param v0,v1,v2      局部坐标系下的三角形顶点
     * @param boxHalfExtents AABB 半边长
     * @param epsilon       碰撞容差——扩大 AABB 投影半径使"接近"也视为重叠
     * @return true 表示两投影区间有交集
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean overlapOnAxis(
        Vector3dc axis,
        Vector3dc v0,
        Vector3dc v1,
        Vector3dc v2,
        Vector3dc boxHalfExtents,
        double epsilon
    ) {
        double p0 = axis.x() * v0.x() + axis.y() * v0.y() + axis.z() * v0.z();
        double p1 = axis.x() * v1.x() + axis.y() * v1.y() + axis.z() * v1.z();
        double p2 = axis.x() * v2.x() + axis.y() * v2.y() + axis.z() * v2.z();

        double tMin = Math.min(Math.min(p0, p1), p2);
        double tMax = Math.max(Math.max(p0, p1), p2);

        double r = Math.abs(axis.x() * boxHalfExtents.x())
                + Math.abs(axis.y() * boxHalfExtents.y())
                + Math.abs(axis.z() * boxHalfExtents.z());

        return !(tMin > r + epsilon) && !(-r > tMax + epsilon);
    }
}
