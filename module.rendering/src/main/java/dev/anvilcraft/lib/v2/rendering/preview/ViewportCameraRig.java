package dev.anvilcraft.lib.v2.rendering.preview;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 结构预览渲染使用的相机参数器。
 *
 * <p>用于构建正交投影与视图矩阵，并尽量贴近原版物品/方块 GUI 的默认朝向。</p>
 */
public class ViewportCameraRig {
    @Getter
    @Setter
    private float zoom = 1;
    private final Vector4f viewport = new Vector4f();
    private Mode mode = Mode.ORTOGRAPHIC;
    @Setter
    @Getter
    private float rotationX;
    @Setter
    @Getter
    private float rotationY;
    @Setter
    @Getter
    private float rotationZ;
    private final Vector3f rotationCenter = new Vector3f();
    @Setter
    @Getter
    private float offsetX;
    @Setter
    @Getter
    private float offsetY;

    /**
     * 根据界面尺寸配置以原点为中心的正交视口。
     */
    public void configureViewport(int width, int height) {
        var halfWidth = width / 2f;
        var halfHeight = height / 2f;
        var renderViewport = new Vector4f(
            -halfWidth,
            -halfHeight,
            halfWidth,
            halfHeight
        );
        this.viewport.set(renderViewport);
    }

    /**
     * 创建默认等距视角预设的相机。
     */
    public ViewportCameraRig() {
        this.applyPreset(PerspectivePreset.ISOMETRIC_NORTH_WEST);
    }

    /**
     * 应用一个预定义的预览相机朝向。
     */
    public void applyPreset(PerspectivePreset preset) {
        switch (preset) {
            case ISOMETRIC_NORTH_EAST -> setOrthographicAngles(225, 30, 0);
            case ISOMETRIC_NORTH_WEST -> setOrthographicAngles(135, 30, 0);
            case UP -> setOrthographicAngles(120, 0, 45);
        }
    }

    /**
     * 在正交模式下设置偏航/俯仰/滚转角度（单位：度）。
     */
    public void setOrthographicAngles(float yawDeg, float pitchDeg, float rollDeg) {
        this.mode = Mode.ORTOGRAPHIC;
        rotationY = yawDeg;
        rotationX = pitchDeg;
        rotationZ = rollDeg;
    }

    /**
     * 构建供 RenderSystem 使用的视图矩阵。
     */
    public Matrix4f buildViewMatrix() {
        var result = new Matrix4f();

        result.translate(offsetX, offsetY, 0);

        // 0.625f 来源于原版方块模型 JSON 的 GUI 默认变换比例
        result.scale(0.625f * 16 * zoom, 0.625f * 16 * zoom, 0.625f * 16 * zoom);

        if (mode == Mode.ORTOGRAPHIC) {
            result.translate(rotationCenter.x, rotationCenter.y, rotationCenter.z);
            result.rotateZ(Mth.DEG_TO_RAD * rotationZ);
            result.rotateX(Mth.DEG_TO_RAD * rotationX);
            result.rotateY(Mth.DEG_TO_RAD * rotationY);
            result.translate(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z);
        }

        return result;
    }

    /**
     * 基于当前视口参数构建正交投影矩阵。
     */
    public Matrix4f buildProjectionMatrix() {
        var projectionMatrix = new Matrix4f();
        projectionMatrix.setOrtho(
            viewport.x(),
            viewport.z(),
            viewport.y(),
            viewport.w(),
            -1000,
            3000
        );
        return projectionMatrix;
    }

    private enum Mode {
        ORTOGRAPHIC,
        PERSPECTIVE
    }

    public enum PerspectivePreset implements StringRepresentable {
        ISOMETRIC_NORTH_EAST("isometric-north-east"),
        ISOMETRIC_NORTH_WEST("isometric-north-west"),
        UP("up");

        private final String serializedName;

        PerspectivePreset(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
