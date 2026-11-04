package dev.anvilcraft.lib.v2.ui.flexible;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
    private static final int POINTER_INSET = 2;

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
            FlexibleSprites.PointerSprite pointer = FlexibleSprites.pickPointer(
                FlyoutRenderer.this.theme,
                FlyoutRenderer.this.style,
                FlyoutRenderer.this.type,
                FlyoutRenderer.this.variant.apply(this.self()),
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
                pointerX = x + Math.round(percent * Math.max(0, width - pointerWidth));
                pointerY = this.pointerDirection == PointerDirection.UP
                    ? y - pointerHeight + FlyoutRenderer.POINTER_INSET
                    : y + height - FlyoutRenderer.POINTER_INSET - 1 - (FlyoutRenderer.this.getType() == SpriteType.SHADED_2PX ? 1 : 0);
            } else {
                pointerY = y + Math.round(percent * Math.max(0, height - pointerHeight));
                pointerX = this.pointerDirection == PointerDirection.LEFT
                    ? x - pointerWidth + FlyoutRenderer.POINTER_INSET
                    : x + width - FlyoutRenderer.POINTER_INSET;
            }
            this.blitPointer(
                graphics, pointer.texture(), pointerX, pointerY, pointerWidth, pointerHeight, pointer.mirrored()
            );
        }

        /**
         * 按原始像素尺寸绘制指针贴图。
         *
         * <p>指针贴图是普通纹理而非九宫格切图，无法走 {@code blitSprite}，改用按普通纹理铺一次的
         * {@code blit}。朝右的指针没有独立贴图，由朝左的贴图水平镜像得到：把姿态原点平移到右边界再沿
         * X 轴取负，等价于对调左右两列的 U 坐标，顶点绕序保持不变，因此不必改动背面剔除状态。
         */
        private void blitPointer(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            boolean mirrored
        ) {
            if (!mirrored) {
                graphics.blit(texture, x - 1, y - 1, 0.0F, 0.0F, width + 2, height + 2, width + 2, height + 2);
                return;
            }
            // 镜像改用翻转 UV：直接对调左右两侧的纹理坐标，顶点绕序与剔除状态都与普通绘制一致。
            graphics.blit(
                texture,
                x - 1,
                y - 1,
                width + 2,
                height + 2,
                (float) (width + 2),
                0.0F,
                -(width + 2),
                height + 2,
                width + 2,
                height + 2
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
