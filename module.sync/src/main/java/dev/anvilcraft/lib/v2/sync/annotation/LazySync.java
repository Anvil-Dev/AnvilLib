package dev.anvilcraft.lib.v2.sync.annotation;

import dev.anvilcraft.lib.v2.sync.util.SyncDirection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注于字段，表示该字段以「惰性」方式同步。
 *
 * <p>与 {@link Sync} 不同，被 {@code @LazySync} 标注的字段无需使用
 * {@link dev.anvilcraft.lib.v2.sync.management.SyncProxy SyncProxy} 包装，
 * 而是由 {@link dev.anvilcraft.lib.v2.sync.management.LazySyncManager LazySyncManager}
 * 在每 tick 结束时扫描其所属对象、对比快照检测变更，并按对象分组同步
 * （即每个对象在同一 tick 内仅发送一个网络包）。</p>
 *
 * <p>所属对象需可被注册的 {@link dev.anvilcraft.lib.v2.sync.management.SyncRegisterEntry
 * SyncRegisterEntry} 解析（如实体、方块实体或静态字段）。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LazySync {
    /**
     * 同步方向。
     *
     * @return 同步方向，默认 {@link SyncDirection#BOTH}
     */
    SyncDirection value() default SyncDirection.BOTH;
}
