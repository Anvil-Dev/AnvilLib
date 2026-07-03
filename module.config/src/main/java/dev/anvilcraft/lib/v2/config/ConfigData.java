package dev.anvilcraft.lib.v2.config;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.util.FormattingUtil;
import dev.anvilcraft.lib.v2.config.util.TranslatableEnum;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ConfigData {
    public static final String TITLE_STRING = "%s.configuration.title";
    public static final String ENUM_STRING = "anvillib.configuration.enum.%s.%s";
    public static final String TOML_STRING = "%s.configuration.section.%s.%s.toml";
    public static final String TOML_TITLE_STRING = "%s.configuration.section.%s.%s.toml.title";
    public static final String OPTION_STRING = "%s.configuration.%s";
    public static final String TOOLTIP_STRING = OPTION_STRING + ".tooltip";
    public static final String COLLAPSIBLE_OBJECT_BUTTON_STRING = "%s.configuration.%s.button";
    public static final String COLLAPSIBLE_OBJECT_TOOLTIP_STRING = "%s.configuration.%s.tooltip";

    public static final Set<String> ADDED = new HashSet<>();

    private record ProviderProxy(LanguageProvider provider) {
        public void add(String key, String value) {
            if (ConfigData.ADDED.contains(key)) return;
            provider.add(key, value);
            ConfigData.ADDED.add(key);
        }
    }

    public static void readConfigClass(LanguageProvider provider, Class<?> configClass) {
        if (!configClass.isAnnotationPresent(Config.class)) return;
        Config config = configClass.getAnnotation(Config.class);
        ProviderProxy proxy = new ProviderProxy(provider);
        String name = config.name();
        ModConfig.Type type = config.type();
        proxy.add(TITLE_STRING.formatted(name), "%s Configuration".formatted(FormattingUtil.toEnglishName(name)));
        proxy.add(
            TOML_STRING.formatted(name, FormattingUtil.toPointSplitName(name), type.extension()),
            "%s %s Configuration".formatted(FormattingUtil.toEnglishName(name), FormattingUtil.toEnglishName(type.extension()))
        );
        proxy.add(
            TOML_TITLE_STRING.formatted(name, FormattingUtil.toPointSplitName(name), type.extension()),
            "%s %s Configuration".formatted(FormattingUtil.toEnglishName(name), FormattingUtil.toEnglishName(type.extension()))
        );
        ConfigData.readConfigClass(proxy, name, type, configClass, null);
    }

    private static void readConfigClass(
        ProviderProxy proxy,
        String modId,
        ModConfig.Type type,
        Class<?> configClass,
        @Nullable String parent
    ) {
        ConfigData.providerAdd(proxy, modId, type, configClass.getDeclaredFields(), parent);
    }

    private static void providerAdd(ProviderProxy proxy, String modId, ModConfig.Type type, Field[] fields, @Nullable String parent) {
        for (Field field : fields) {
            String fieldName = FormattingUtil.toLowerCaseUnder(field.getName());
            String name;
            if (field.isAnnotationPresent(SerializedName.class)) {
                name = field.getAnnotation(SerializedName.class).value();
            } else {
                name = FormattingUtil.toEnglishName(fieldName);
            }
            if (parent != null) fieldName = parent + "." + fieldName;
            boolean flag = field.isAnnotationPresent(CollapsibleObject.class);
            Class<?> fieldType = field.getType();
            if (flag) {
                proxy.add(COLLAPSIBLE_OBJECT_BUTTON_STRING.formatted(modId, fieldName), name);
                proxy.add(COLLAPSIBLE_OBJECT_TOOLTIP_STRING.formatted(modId, fieldName), name);
                ConfigData.readConfigClass(proxy, modId, type, fieldType, fieldName);
            }
            proxy.add(OPTION_STRING.formatted(modId, fieldName), name);
            if (field.isAnnotationPresent(Comment.class)) {
                Comment comment = field.getAnnotation(Comment.class);
                proxy.add(TOOLTIP_STRING.formatted(modId, fieldName), comment.value());
            }
            if (Enum.class.isAssignableFrom(fieldType) && TranslatableEnum.class.isAssignableFrom(fieldType)) {
                ConfigData.enumValueAdd(proxy, cast(fieldType));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object t) {
        return (T) t;
    }

    private static <T extends Enum<T> & TranslatableEnum> void enumValueAdd(ProviderProxy proxy, Class<T> clazz) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isEnumConstant()) continue;
                T value = cast(field.get(null));
                String name;
                if (field.isAnnotationPresent(SerializedName.class)) {
                    name = field.getAnnotation(SerializedName.class).value();
                } else {
                    name = FormattingUtil.toEnglishName(value.name());
                }
                String key = value.getTranslationKey();
                proxy.add(key, name);
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to add enum values for {}", clazz.getName(), e);
        }
    }
}
