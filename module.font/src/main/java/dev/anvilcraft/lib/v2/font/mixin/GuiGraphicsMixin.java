package dev.anvilcraft.lib.v2.font.mixin;

import dev.anvilcraft.lib.v2.font.extension.GuiGraphicsExtension;
import dev.anvilcraft.lib.v2.font.sdf.SdfTextRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ApiStatus.Internal
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements GuiGraphicsExtension {
    @Unique
    SdfTextRenderer anvillib$textRenderer = new SdfTextRenderer();

    public SdfTextRenderer anvillib$textRenderer() {
        return this.anvillib$textRenderer;
    }
}
