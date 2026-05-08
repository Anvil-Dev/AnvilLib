package dev.anvilcraft.lib.v2.font.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

public class FontConfigScreen extends Screen {
    protected final Screen lastScreen;

    public FontConfigScreen(final ModContainer ignored, final Screen parent) {
        super(Component.translatable("screen.anvillib_font.config"));
        this.lastScreen = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
