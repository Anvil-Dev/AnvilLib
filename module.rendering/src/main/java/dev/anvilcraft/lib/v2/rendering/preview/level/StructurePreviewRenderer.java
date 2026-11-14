package dev.anvilcraft.lib.v2.rendering.preview.level;

import dev.anvilcraft.lib.v2.rendering.preview.StructurePreviewPipRenderer;
import dev.anvilcraft.lib.v2.rendering.preview.ViewportCameraRig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * 结构/方块预览渲染器。
 */
public final class StructurePreviewRenderer {
    private static @Nullable StructurePreviewRenderer instance;

    public static StructurePreviewRenderer getInstance() {
        if (instance == null) {
            instance = new StructurePreviewRenderer();
        }
        return instance;
    }

    /**
     * @param graphics 当前绘制上下文
     * @param maxX     组件渲染宽度
     * @param height   组件渲染高度
     */
    public void render(
        SandboxRenderLevel level,
        ViewportCameraRig cameraRig,
        GuiGraphicsExtractor graphics,
        int maxX,
        int height,
        int visibleMinY,
        int visibleMaxYExclusive,
        float panOffsetX,
        float panOffsetY,
        float scale
    ) {
        float parentScale = Math.max((float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11()));
        if (parentScale <= 0 || maxX <= 0 || height <= 0) {
            return;
        }
        float pixelsPerBlock = 10.0f * cameraRig.getZoom();
        var transform = createTransform(cameraRig, panOffsetX, panOffsetY);
        int x0 = Math.round(graphics.pose().m20());
        int y0 = Math.round(graphics.pose().m21());
        int x1 = x0 + Math.max(1, Math.round(Math.min(maxX, 8192) * parentScale));
        int y1 = y0 + Math.max(1, Math.round(Math.min(height, 8192) * parentScale));
        var state = new StructurePreviewPipRenderer.State(level, visibleMinY, visibleMaxYExclusive,
            transform, x0, y0, x1, y1, pixelsPerBlock * parentScale, graphics.peekScissorStack());
        if (state.bounds() != null) {
            graphics.submitPictureInPictureRenderState(state);
        }
    }

    /** 使用与 PiP 绘制一致的局部坐标和相机变换，界面缩放不影响选取。 */
    public @Nullable BlockPos pick(
        SandboxRenderLevel level, ViewportCameraRig cameraRig, int width, int height,
        double mouseX, double mouseY, int minY, int maxY, float panOffsetX, float panOffsetY
    ) {
        float pixelsPerBlock = 10.0f * cameraRig.getZoom();
        if (pixelsPerBlock <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        Matrix4f inverse = createTransform(cameraRig, panOffsetX, panOffsetY).invert();
        float x = (float) (mouseX - width / 2.0) / pixelsPerBlock;
        float y = (float) (height / 2.0 - mouseY) / pixelsPerBlock;
        var bounds = level.getBounds();
        float depth = new Vector3f(
            Math.max(Math.abs(bounds.min().getX()), Math.abs(bounds.max().getX())),
            Math.max(Math.abs(bounds.min().getY()), Math.abs(bounds.max().getY())),
            Math.max(Math.abs(bounds.min().getZ()), Math.abs(bounds.max().getZ()))).length() + 2;
        Vector3f near = inverse.transformPosition(new Vector3f(x, y, depth));
        Vector3f far = inverse.transformPosition(new Vector3f(x, y, -depth));
        return StructurePreviewPicker.pick(level, new Vec3(near), new Vec3(far), minY, maxY);
    }

    /** 与选取、绘制一致的方块中心投影，结果为组件内坐标。 */
    public org.joml.Vector2f projectCenter(BlockPos pos, ViewportCameraRig cameraRig, int width, int height,
                                           float panOffsetX, float panOffsetY) {
        Vector3f point = createTransform(cameraRig, panOffsetX, panOffsetY)
            .transformPosition(new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));
        float pixelsPerBlock = 10 * cameraRig.getZoom();
        return new org.joml.Vector2f(width / 2f + point.x * pixelsPerBlock, height / 2f - point.y * pixelsPerBlock);
    }

    private static Matrix4f createTransform(ViewportCameraRig cameraRig, float panOffsetX, float panOffsetY) {
        float pixelsPerBlock = 10.0f * cameraRig.getZoom();
        return new Matrix4f().translate(panOffsetX / pixelsPerBlock, panOffsetY / pixelsPerBlock, 0)
            .rotateZ(Mth.DEG_TO_RAD * cameraRig.getRotationZ())
            .rotateX(Mth.DEG_TO_RAD * cameraRig.getRotationX())
            .rotateY(Mth.DEG_TO_RAD * cameraRig.getRotationY());
    }
}
