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
    TextAlignment textAlignment
) {
    public static final int MIN_BANNER_LENGTH = 1;
    public static final int MAX_BANNER_LENGTH = 9;
    public static final int DEFAULT_BANNER_LENGTH = 3;

    public CreativeTabSection {
        if (bannerLength < MIN_BANNER_LENGTH || bannerLength > MAX_BANNER_LENGTH) {
            throw new IllegalArgumentException("Banner length must be between 1 and 9");
        }
        bannerTexture = Objects.requireNonNull(bannerTexture, "bannerTexture");
        text = Objects.requireNonNull(text, "text");
        tooltip = List.copyOf(tooltip);
        textAlignment = Objects.requireNonNull(textAlignment, "textAlignment");
    }

    public CreativeTabSection(
        int bannerLength,
        ResourceLocation bannerTexture,
        Component text,
        int textBackgroundColor,
        List<Component> tooltip
    ) {
        this(bannerLength, bannerTexture, text, textBackgroundColor, tooltip, TextAlignment.CENTER);
    }

    public static Builder builder(ResourceLocation bannerTexture) {
        return new Builder(bannerTexture);
    }

    public static final class Builder {
        private final ResourceLocation bannerTexture;
        private int bannerLength = DEFAULT_BANNER_LENGTH;
        private Component text = Component.empty();
        private int textBackgroundColor = 0x00000000;
        private List<Component> tooltip = List.of();
        private TextAlignment textAlignment = TextAlignment.CENTER;

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
                this.textAlignment
            );
        }
    }

    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
}
