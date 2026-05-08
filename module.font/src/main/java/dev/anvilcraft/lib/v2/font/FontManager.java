package dev.anvilcraft.lib.v2.font;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.UIManager;

@Slf4j
@Getter
public class FontManager {
    public static final FontManager INSTANCE = new FontManager();
    private final Map<String, Set<Font>> familyMap = new HashMap<>();
    private final Font defaultFont;

    private FontManager() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = graphicsEnvironment.getAllFonts();
        for (Font font : allFonts) {
            String familyName = font.getFamily();
            familyMap.computeIfAbsent(familyName, _ -> new java.util.HashSet<>()).add(font);
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
        this.defaultFont = UIManager.getFont("Label.font");
    }

    public Collection<String> getFamilyNames() {
        return this.familyMap.keySet();
    }

    public Collection<String> getFamilyFontNames(String familyName) {
        return this.familyMap.get(familyName).stream().map(Font::getFontName).toList();
    }
}
