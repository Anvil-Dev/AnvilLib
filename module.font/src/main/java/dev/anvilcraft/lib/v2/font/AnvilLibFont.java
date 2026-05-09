package dev.anvilcraft.lib.v2.font;

import dev.anvilcraft.lib.v2.font.screen.FontConfigScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.awt.Font;

@EventBusSubscriber
@Mod(value = AnvilLibFont.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibFont {
    public static final String MOD_ID = "anvillib_font";
    public static final AnvilLibFontConfig CONFIG = new AnvilLibFontConfig();

    public AnvilLibFont(ModContainer container) {
        AnvilLibFontConfig.AnvilLibFontConfigManager.readConfig(AnvilLibFont.CONFIG);
        container.registerExtensionPoint(IConfigScreenFactory.class, FontConfigScreen::new);
    }

    public static Font getSelectFont() {
        return FontManager.INSTANCE.getFont(AnvilLibFont.CONFIG.getFont());
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibFont.MOD_ID, path);
    }
}
