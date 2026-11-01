package dev.anvilcraft.lib.v2.config;

import net.neoforged.fml.config.ModConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
    String name();

    String group() default "";

    ModConfig.Type type() default ModConfig.Type.COMMON;

    /** 通过 ConfigManager 注册时自动选择此配置文件的界面。 */
    ConfigScreenStyle screenStyle() default ConfigScreenStyle.DEFAULT;
}

