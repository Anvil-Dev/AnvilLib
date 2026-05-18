package dev.anvilcraft.lib.v2.ui;

/**
 * 子组件在交叉轴上的对齐方式。
 */
public final class Alignment {
    private Alignment() {
    }

    /**
     * 水平对齐（Column 中每个子组件的 X 定位）。
     */
    public enum Horizontal {
        Start, Center, End;

        /**
         * @param totalWidth 父容器宽度
         * @param childWidth 子组件宽度
         * @return 子组件的 x 偏移
         */
        public float align(float totalWidth, float childWidth) {
            return switch (this) {
                case Start -> 0;
                case Center -> (totalWidth - childWidth) / 2;
                case End -> totalWidth - childWidth;
            };
        }
    }

    /**
     * 垂直对齐（Row 中每个子组件的 Y 定位）。
     */
    public enum Vertical {
        Top, Center, Bottom;

        /**
         * @param totalHeight 父容器高度
         * @param childHeight 子组件高度
         * @return 子组件的 y 偏移
         */
        public float align(float totalHeight, float childHeight) {
            return switch (this) {
                case Top -> 0;
                case Center -> (totalHeight - childHeight) / 2;
                case Bottom -> totalHeight - childHeight;
            };
        }
    }
}
