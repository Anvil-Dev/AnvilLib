package dev.anvilcraft.lib.v2.font;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.UIManager;

@Slf4j
@Getter
public class FontManager {
    public static final FontManager INSTANCE = new FontManager();
    private final Map<String, Font> fontMap = new HashMap<>();
    private final Multimap<String, Font> fontMultimap;
    private final Font defaultFont;

    private FontManager() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = graphicsEnvironment.getAllFonts();
        for (Font font : allFonts) {
            fontMap.put(font.getFontName(), font);
        }
        this.fontMultimap = FontTrieNode.process(this.fontMap.values());
        log.info("fontMultimap: {}", this.fontMultimap);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
        this.defaultFont = UIManager.getFont("Label.font");
    }

    public Collection<String> getFamilyNames() {
        return this.fontMultimap.keySet();
    }

    public Collection<String> getFamilyFontNames(String familyName) {
        if (!this.fontMultimap.containsKey(familyName)) return Set.of();
        return this.fontMultimap.get(familyName).stream().map(Font::getFontName).toList();
    }

    public Font getFont(@Nullable String name) {
        return name == null || !this.fontMap.containsKey(name) ? this.getDefaultFont() : this.fontMap.get(name);
    }

    static class FontTrieNode {
        final Map<Character, FontTrieNode> children = new TreeMap<>();
        @Nullable Font value;

        void append(String key, Font font) {
            if (key.isEmpty()) return;
            if (key.length() == 1) {
                this.value = font;
                return;
            }
            char prefix = key.charAt(0);
            String suffix = key.substring(1);
            this.children.computeIfAbsent(prefix, _ -> new FontTrieNode()).append(suffix, font);
        }

        void subFont(Set<Font> set) {
            if (this.value != null) {
                set.add(this.value);
            }
            this.children.forEach((prefixChar, child) -> child.subFont(set));
        }

        void result(String prefix, Multimap<String, Font> result) {
            if (this.value != null) {
                Set<Font> set = new TreeSet<>(Comparator.comparing(Font::getFontName));
                this.subFont(set);
                result.putAll(this.value.getFontName(), set);
                return;
            }
            // Only group at word boundaries: prefix ending with space & multiple descendants
            if (prefix.endsWith(" ")) {
                Set<Font> set = new TreeSet<>(Comparator.comparing(Font::getFontName));
                this.subFont(set);
                if (set.size() > 1) {
                    result.putAll(prefix.stripTrailing(), set);
                    return;
                }
            }
            this.children.forEach((prefixChar, child) -> child.result(prefix + prefixChar, result));
        }

        static Multimap<String, Font> process(Collection<Font> fonts) {
            FontTrieNode root = new FontTrieNode();
            fonts.forEach(font -> root.append(font.getFontName(), font));
            Multimap<String, Font> result = MultimapBuilder.hashKeys().hashSetValues().build();
            root.result("", result);
            return result;
        }
    }
}
