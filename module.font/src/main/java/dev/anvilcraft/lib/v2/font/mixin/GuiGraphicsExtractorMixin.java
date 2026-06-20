package dev.anvilcraft.lib.v2.font.mixin;

import dev.anvilcraft.lib.v2.font.extension.GuiGraphicsExtractorExtension;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ApiStatus.Internal
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GuiGraphicsExtractorExtension {
    @Unique
    SdfTextRenderer anvillib$textRenderer = new SdfTextRenderer();

    public SdfTextRenderer anvillib$textRenderer() {
        return this.anvillib$textRenderer;
    }
}

