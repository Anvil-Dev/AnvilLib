package dev.anvilcraft.lib.v2.sync.annotation;

import dev.anvilcraft.lib.v2.sync.util.SyncDirection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Sync {
    SyncDirection value() default SyncDirection.BOTH;
}
