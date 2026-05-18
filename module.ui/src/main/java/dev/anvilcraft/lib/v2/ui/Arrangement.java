package dev.anvilcraft.lib.v2.ui;

import java.util.List;

/**
 * 子组件在主轴上的分布方式。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public final class Arrangement {
    private Arrangement() {
    }

    /**
     * 纵向排列（Column 主轴）。
     */
    public enum Vertical {
        Top, Center, Bottom, SpaceBetween, SpaceAround, SpaceEvenly;

        /**
         * @param totalHeight  可用总高度
         * @param childHeights 各子组件高度
         * @param spacing      间距
         * @return 各子组件的 y 偏移
         */
        public float[] arrange(float totalHeight, List<Float> childHeights, float spacing) {
            int n = childHeights.size();
            if (n == 0) return new float[0];

            float content = 0;
            for (float h : childHeights) content += h;
            float gapTotal = spacing * (n - 1);
            float extra = totalHeight - content - gapTotal;

            float[] offsets = new float[n];
            switch (this) {
                case Vertical.Top -> {
                    float y = 0;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = y;
                        y += childHeights.get(i) + spacing;
                    }
                }
                case Vertical.Center -> {
                    float y = Math.max(0, extra / 2);
                    for (int i = 0; i < n; i++) {
                        offsets[i] = y;
                        y += childHeights.get(i) + spacing;
                    }
                }
                case Vertical.Bottom -> {
                    float y = Math.max(0, extra);
                    for (int i = 0; i < n; i++) {
                        offsets[i] = y;
                        y += childHeights.get(i) + spacing;
                    }
                }
                case Vertical.SpaceBetween -> {
                    float gap = n > 1 ? (extra + gapTotal) / (n - 1) : 0;
                    float y = 0;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = y;
                        y += childHeights.get(i) + gap;
                    }
                }
                case Vertical.SpaceAround -> {
                    float halfGap = n > 0 ? (extra + gapTotal) / (n * 2f) : 0;
                    float y = halfGap;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = y;
                        y += childHeights.get(i) + spacing + halfGap * 2 - spacing;
                    }
                }
                case Vertical.SpaceEvenly -> {
                    float gap = n > 0 ? (extra + gapTotal) / (n + 1) : 0;
                    float y = gap;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = y;
                        y += childHeights.get(i) + spacing + gap - spacing;
                    }
                }
            }
            return offsets;
        }
    }

    /**
     * 横向排列（Row 主轴）。
     */
    public enum Horizontal {
        Start, Center, End, SpaceBetween, SpaceAround, SpaceEvenly;

        /**
         * @param totalWidth  可用总宽度
         * @param childWidths 各子组件宽度
         * @param spacing     间距
         * @return 各子组件的 x 偏移
         */
        public float[] arrange(float totalWidth, List<Float> childWidths, float spacing) {
            int n = childWidths.size();
            if (n == 0) return new float[0];

            float content = 0;
            for (float w : childWidths) content += w;
            float gapTotal = spacing * (n - 1);
            float extra = totalWidth - content - gapTotal;

            float[] offsets = new float[n];
            switch (this) {
                case Horizontal.Start -> {
                    float x = 0;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = x;
                        x += childWidths.get(i) + spacing;
                    }
                }
                case Horizontal.Center -> {
                    float x = Math.max(0, extra / 2);
                    for (int i = 0; i < n; i++) {
                        offsets[i] = x;
                        x += childWidths.get(i) + spacing;
                    }
                }
                case Horizontal.End -> {
                    float x = Math.max(0, extra);
                    for (int i = 0; i < n; i++) {
                        offsets[i] = x;
                        x += childWidths.get(i) + spacing;
                    }
                }
                case Horizontal.SpaceBetween -> {
                    float gap = n > 1 ? (extra + gapTotal) / (n - 1) : 0;
                    float x = 0;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = x;
                        x += childWidths.get(i) + gap;
                    }
                }
                case Horizontal.SpaceAround -> {
                    float halfGap = n > 0 ? (extra + gapTotal) / (n * 2f) : 0;
                    float x = halfGap;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = x;
                        x += childWidths.get(i) + spacing + halfGap * 2 - spacing;
                    }
                }
                case Horizontal.SpaceEvenly -> {
                    float gap = n > 0 ? (extra + gapTotal) / (n + 1) : 0;
                    float x = gap;
                    for (int i = 0; i < n; i++) {
                        offsets[i] = x;
                        x += childWidths.get(i) + spacing + gap - spacing;
                    }
                }
            }
            return offsets;
        }
    }
}
