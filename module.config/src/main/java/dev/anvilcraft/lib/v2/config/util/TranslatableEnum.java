package dev.anvilcraft.lib.v2.config.util;

import dev.anvilcraft.lib.v2.config.ConfigData;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface TranslatableEnum extends net.neoforged.neoforge.common.TranslatableEnum {
    @Override
    default Component getTranslatedName() {
        return Component.translatableWithFallback(this.getTranslationKey(), ((Enum<?>) this).name());
    }

    default String getTranslationKey() {
        String className = Arrays.stream(this.getClass().getCanonicalName().split("[.$]"))
            .map(FormattingUtil::toLowerCaseUnder)
            .collect(Collectors.joining("."));
        String valueName = FormattingUtil.toLowerCaseUnder(((Enum<?>) this).name().toLowerCase());
        return ConfigData.ENUM_STRING.formatted(className, valueName);
    }
}
