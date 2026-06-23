package dev.anvilcraft.lib.v2.config.util;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public abstract class FormattingUtil {
    /**
     * 与 .to(LOWER_UNDERSCORE, string) 几乎相同，但它也会在单词和数字之间插入下划线。
     *
     * @param string 任何带有 ASCII 字符的字符串。
     * @return 全小写的字符串，在单词/数字边界前插入下划线：“maragingSteel300” -> “maraging_steel_300”
     */
    public static String toLowerCaseUnder(String string) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, string);
    }

    /**
     * apple_orange.juice => Apple Orange (Juice)
     */
    public static String toEnglishName(Object internalName) {
        return Arrays.stream(internalName.toString().toLowerCase(Locale.ROOT).split("_"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    public static String toPointSplitName(String name) {
        return Arrays.stream(name.split("[^A-Za-z0-9]"))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining("."));
    }
}
