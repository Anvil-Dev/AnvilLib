package dev.anvilcraft.lib.v2.ui.flexible;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * 带指针的浮窗渲染器。
 *
 * <p>与 {@link ButtonRenderer} 的唯一区别是多画一枚指示指向对象的指针。指针贴图与本体共用主题、
 * 样式、类型与变体，因此外观始终跟随本体；指针具体贴在本体的哪条边上由 {@link PointerDirection} 决定。
 *
 * <p>指针位置用百分比描述，取值 0 到 1 线性映射到指针沿所贴边能走的长度。这段长度就是本体在该边上的
 * 尺寸减去指针自身的尺寸：指针中心从贴着本体的一端滑到另一端，指针整体始终落在本体轮廓之内，
 * 既不会探出本体，也不会盖住本体之外的阴影。
 *
 * <p>垂直于所贴边的方向上，指针整枚画在本体之外并紧贴该边，因此不会覆盖本体内容与边框。
 */
public class FlyoutRenderer extends ButtonRenderer {
    /**
     * 指针沿垂直于所贴边的方向向内收进的像素数，避免箭头根部与本体边框完全贴边。
     */
    private static final int POINTER_INSET = 1;

    /**
     * 指针沿所贴边方向的移位余量，两端各留出这段距离。
     */
    private static final int POINTER_MARGIN = 2;

    /**
     * 计算指针贴图的绘制矩形。
     *
     * <p>{@code width} 与 {@code height} 是可见内容尺寸，贴图四周各有 1px 透明边，因此绘制矩形
     * 向左上各扩 1px、宽高各加 2px。
     */
    public static PointerQuad pointerQuad(int x, int y, int width, int height) {
        return new PointerQuad(x - 1, y - 1, width + 2, height + 2);
    }

    /**
     * 指针贴图的一次绘制参数。
     *
     * @param x      绘制矩形左边界
     * @param y      绘制矩形上边界
     * @param width  绘制矩形宽度
     * @param height 绘制矩形高度
     */
    public record PointerQuad(int x, int y, int width, int height) {
    }

    /**
     * 按顶点色绘制一枚贴图。
     *
     * <p>1.21.1 的 {@code GuiGraphics.blit} 没有颜色参数，只能靠全局着色器颜色染色；指针贴图是普通纹理，
     * 走那条路径时透明度不会生效，因此这里直接按 {@code POSITION_TEX_COLOR} 顶点格式铺一个四边形，
     * 把颜色写在顶点上。
     *
     * <p>顶点色会与当前全局 {@code ColorModulator} 相乘，指针因此继承父级界面的整体染色；
     * 如果需要完全独立的指针颜色，调用前应先把着色器颜色重置为白色并在结束后恢复。
     */
    private static void blitTinted(
        GuiGraphics graphics,
        ResourceLocation texture,
        int x,
        int y,
        int width,
        int height,
        float u0,
        float u1,
        float v0,
        float v1,
        int argb
    ) {
        float alpha = (argb >>> 24 & 0xFF) / 255.0F;
        float red = (argb >>> 16 & 0xFF) / 255.0F;
        float green = (argb >>> 8 & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        graphics.flush();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(pose, (float) x, (float) y, 0.0F)
            .setUv(u0, v0)
            .setColor(red, green, blue, alpha);
        buffer.addVertex(pose, (float) x, (float) (y + height), 0.0F)
            .setUv(u0, v1)
            .setColor(red, green, blue, alpha);
        buffer.addVertex(pose, (float) (x + width), (float) (y + height), 0.0F)
            .setUv(u1, v1)
            .setColor(red, green, blue, alpha);
        buffer.addVertex(pose, (float) (x + width), (float) y, 0.0F)
            .setUv(u1, v0)
            .setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    @Override
    public Scope open() {
        return new Scope();
    }

    public class Scope extends ButtonRenderer.Scope {
        private @Nullable PointerDirection pointerDirection;
        private @Nullable Float pointerPercent;

        /**
         * 一次设置指针朝向与位置百分比。
         *
         * @param direction 指针所贴的边，同时决定箭头指向
         * @param percent   指针在可移动长度上的位置，超出 0 到 1 时按最近的边界取值
         */
        public Scope pointer(PointerDirection direction, float percent) {
            this.pointerDirection = direction;
            this.pointerPercent = percent;
            return this;
        }

        /**
         * 只设置指针所贴的边。与 {@link #pointerPercent(float)} 一起使用时两者都必须在绘制前给出，
         * 只给其中一个属于参数不全，{@link #render()} 会拒绝绘制。
         */
        public Scope pointerDirection(PointerDirection direction) {
            this.pointerDirection = direction;
            return this;
        }

        /**
         * 只设置指针位置百分比，取值超出 0 到 1 时按最近的边界取值。
         */
        public Scope pointerPercent(float percent) {
            this.pointerPercent = percent;
            return this;
        }

        @Override
        protected void render(GuiGraphics graphics, int x, int y, int width, int height) {
            super.render(graphics, x, y, width, height);
            if (this.pointerDirection == null || this.pointerPercent == null) {
                return;
            }
            SpriteVariant variant = FlyoutRenderer.this.variant.apply(this.self());
            FlexibleSprites.PointerSprite pointer = FlexibleSprites.pickPointer(
                FlyoutRenderer.this.theme,
                FlyoutRenderer.this.style,
                FlyoutRenderer.this.type,
                variant,
                this.pointerDirection
            );
            if (pointer == null) {
                return;
            }
            float percent = Mth.clamp(this.pointerPercent, 0.0F, 1.0F);
            int pointerWidth = pointer.width();
            int pointerHeight = pointer.height();
            int pointerX;
            int pointerY;
            if (this.pointerDirection == PointerDirection.UP || this.pointerDirection == PointerDirection.DOWN) {
                pointerX = x + FlyoutRenderer.POINTER_MARGIN
                    + Math.round(percent * Math.max(0, width - pointerWidth - FlyoutRenderer.POINTER_MARGIN * 2));
                pointerY = this.pointerDirection == PointerDirection.UP
                    ? y - pointerHeight + FlyoutRenderer.POINTER_INSET + pointer.pointerUpInset()
                    : y + height - this.pointerDownInset();
            } else {
                pointerY = y + FlyoutRenderer.POINTER_MARGIN
                    + Math.round(percent * Math.max(0, height - pointerHeight - FlyoutRenderer.POINTER_MARGIN * 2));
                pointerX = this.pointerDirection == PointerDirection.LEFT
                    ? x - pointerWidth + FlyoutRenderer.POINTER_INSET
                    : x + width - FlyoutRenderer.POINTER_INSET;
            }
            this.blitPointer(graphics, pointer.texture(), pointerX, pointerY, pointerWidth, pointerHeight);
        }

        /**
         * 下指针贴底时的收进量：阴影层数越多，贴图底部多出的行越多，需要多收进对应像素。
         */
        private int pointerDownInset() {
            return switch (FlyoutRenderer.this.getType()) {
                case NON_SHADED -> 1;
                case SHADED_1PX -> 2;
                case SHADED_2PX -> 3;
            };
        }

        /**
         * 按原始像素尺寸绘制指针贴图。
         *
         * <p>指针贴图是普通纹理而非九宫格切图，无法走 {@code blitSprite}，改用按普通纹理铺一次的
         * {@code blit}。
         */
        private void blitPointer(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height
        ) {
            FlyoutRenderer.PointerQuad quad = FlyoutRenderer.pointerQuad(x, y, width, height);
            FlyoutRenderer.blitTinted(
                graphics,
                texture,
                quad.x(),
                quad.y(),
                quad.width(),
                quad.height(),
                0.0F,
                1.0F,
                0.0F,
                1.0F,
                ButtonRenderer.color(FlyoutRenderer.this.alpha)
            );
        }

        @Override
        protected String validate(String missing) {
            if (this.pointerDirection == null && this.pointerPercent != null) {
                return "pointerDirection";
            }
            if (this.pointerDirection != null && this.pointerPercent == null) {
                return "pointerPercent";
            }
            return "";
        }
    }
}
