package dev.anvilcraft.lib.v2.font;

import dev.anvilcraft.lib.v2.font.screen.FontConfigScreen;
import dev.anvilcraft.lib.v2.font.sdf.SdfGlyphAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.ApiStatus;

import java.awt.Font;
import java.util.List;

@EventBusSubscriber
@Mod(value = AnvilLibFont.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibFont {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_font";
    public static final AnvilLibFontConfig CONFIG = new AnvilLibFontConfig();

    @ApiStatus.Internal
    public AnvilLibFont(ModContainer container) {
        AnvilLibFontConfig.AnvilLibFontConfigManager.readConfig(AnvilLibFont.CONFIG);
        container.registerExtensionPoint(IConfigScreenFactory.class, FontConfigScreen::new);
        // Start building the SDF atlas for the base font and all common style
        // variants on background threads at mod init time.  This avoids render-
        // thread blocking (visible lag) when bold/italic text is first drawn.
        Font base = getSelectFont();
        SdfGlyphAtlas.getOrCreate(base);
        for (int style : List.of(Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC)) {
            SdfGlyphAtlas.getOrCreate(base.deriveFont(style));
        }
    }

    public static Font getSelectFont() {
        Font font = FontManager.INSTANCE.getFont(AnvilLibFont.CONFIG.getFont());
        // Eagerly start building the SDF atlas on a background thread so it is
        // ready (or nearly ready) by the time the first text is rendered.
        SdfGlyphAtlas.getOrCreate(font);
        return font;
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibFont.MOD_ID, path);
    }
}
