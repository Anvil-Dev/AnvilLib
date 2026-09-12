package dev.anvilcraft.lib.v2.registrum.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record CreativeTabSection(
    int bannerLength,
    ResourceLocation bannerTexture,
    Component text,
    int textBackgroundColor,
    List<Component> tooltip,
    TextAlignment textAlignment,
    int textStart,
    int textEnd
) {
    public static final int MIN_BANNER_LENGTH = 1;
    public static final int MAX_BANNER_LENGTH = 9;
    public static final int DEFAULT_BANNER_LENGTH = 3;
    public static final int BANNER_CELL_SIZE = 18;
    public static final int DEFAULT_TEXT_PADDING = 2;

    public CreativeTabSection {
        validateBannerLength(bannerLength);
        bannerTexture = Objects.requireNonNull(bannerTexture, "bannerTexture");
        text = Objects.requireNonNull(text, "text");
        tooltip = List.copyOf(tooltip);
        textAlignment = Objects.requireNonNull(textAlignment, "textAlignment");
        validateTextRange(bannerLength, textStart, textEnd);
    }

    public CreativeTabSection(
        int bannerLength,
        ResourceLocation bannerTexture,
        Component text,
        int textBackgroundColor,
        List<Component> tooltip
    ) {
        this(
            bannerLength,
            bannerTexture,
            text,
            textBackgroundColor,
            tooltip,
            TextAlignment.CENTER,
            DEFAULT_TEXT_PADDING,
            defaultTextEnd(bannerLength)
        );
    }

    public CreativeTabSection(
        int bannerLength,
        ResourceLocation bannerTexture,
        Component text,
        int textBackgroundColor,
        List<Component> tooltip,
        TextAlignment textAlignment
    ) {
        this(
            bannerLength,
            bannerTexture,
            text,
            textBackgroundColor,
            tooltip,
            textAlignment,
            DEFAULT_TEXT_PADDING,
            defaultTextEnd(bannerLength)
        );
    }

    public static Builder builder(ResourceLocation bannerTexture) {
        return new Builder(bannerTexture);
    }

    /** 检查是否使用默认的两像素横幅文字内边距。 */
    public boolean hasDefaultTextRange() {
        return this.textStart == DEFAULT_TEXT_PADDING
            && this.textEnd == this.bannerLength * BANNER_CELL_SIZE - DEFAULT_TEXT_PADDING;
    }

    public int textWidth() {
        return this.textEnd - this.textStart;
    }

    public int textLeft() {
        return this.textStart;
    }

    public int textRight() {
        return this.textEnd;
    }

    private static int defaultTextEnd(int bannerLength) {
        validateBannerLength(bannerLength);
        return bannerLength * BANNER_CELL_SIZE - DEFAULT_TEXT_PADDING;
    }

    private static void validateBannerLength(int bannerLength) {
        if (bannerLength < MIN_BANNER_LENGTH || bannerLength > MAX_BANNER_LENGTH) {
            throw new IllegalArgumentException("Banner length must be between 1 and 9");
        }
    }

    private static void validateTextRange(int bannerLength, int textStart, int textEnd) {
        if (textStart < 0) {
            throw new IllegalArgumentException("Text range left edge cannot be negative");
        }
        if (textEnd <= textStart) {
            throw new IllegalArgumentException("Text range right edge must be greater than its left edge");
        }
        int bannerWidth = bannerLength * BANNER_CELL_SIZE;
        if (textEnd > bannerWidth) {
            throw new IllegalArgumentException(
                "Text range must fit within the banner width of " + bannerWidth + " pixels"
            );
        }
    }

    public static final class Builder {
        private final ResourceLocation bannerTexture;
        private int bannerLength = DEFAULT_BANNER_LENGTH;
        private Component text = Component.empty();
        private int textBackgroundColor = 0x00000000;
        private List<Component> tooltip = List.of();
        private TextAlignment textAlignment = TextAlignment.CENTER;
        private int textStart = DEFAULT_TEXT_PADDING;
        private Integer textEnd;

        private Builder(ResourceLocation bannerTexture) {
            this.bannerTexture = Objects.requireNonNull(bannerTexture, "bannerTexture");
        }

        public Builder bannerLength(int bannerLength) {
            this.bannerLength = bannerLength;
            return this;
        }

        public Builder text(Component text) {
            this.text = Objects.requireNonNull(text, "text");
            return this;
        }

        public Builder textBackgroundColor(int textBackgroundColor) {
            this.textBackgroundColor = textBackgroundColor;
            return this;
        }

        public Builder textAlignment(TextAlignment textAlignment) {
            this.textAlignment = Objects.requireNonNull(textAlignment, "textAlignment");
            return this;
        }

        /**
         * 设置相对于横幅的左闭右开像素范围；54 像素横幅的 {@code textRange(20, 52)} 会保留左侧装饰。
         */
        public Builder textRange(int textStart, int textEnd) {
            this.textStart = textStart;
            this.textEnd = textEnd;
            return this;
        }

        /** 设置左侧文字缩进，保留默认的两像素右侧缩进。 */
        public Builder textIndent(int textStart) {
            this.textStart = textStart;
            this.textEnd = null;
            return this;
        }

        /** 设置文字区域的左边界和右边界。 */
        public Builder textIndent(int textStart, int textEnd) {
            return this.textRange(textStart, textEnd);
        }

        public Builder textLeft(int textLeft) {
            this.textStart = textLeft;
            return this;
        }

        public Builder textRight(int textRight) {
            this.textEnd = textRight;
            return this;
        }

        public Builder tooltip(Component... tooltip) {
            return this.tooltip(List.of(tooltip));
        }

        public Builder tooltip(List<Component> tooltip) {
            this.tooltip = List.copyOf(tooltip);
            return this;
        }

        public CreativeTabSection build() {
            return new CreativeTabSection(
                this.bannerLength,
                this.bannerTexture,
                this.text,
                this.textBackgroundColor,
                this.tooltip,
                this.textAlignment,
                this.textStart,
                this.textEnd == null ? defaultTextEnd(this.bannerLength) : this.textEnd
            );
        }
    }

    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
}
