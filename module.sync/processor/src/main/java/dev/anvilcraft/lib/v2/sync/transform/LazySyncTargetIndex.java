package dev.anvilcraft.lib.v2.sync.transform;

import org.jetbrains.annotations.ApiStatus;

import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;

/**
 * 在类加载早期扫描所有使用 {@link dev.anvilcraft.lib.v2.sync.annotation.LazySync}
 * 注解的字段，记录其所属类的内部名。
 *
 * <p>{@link SyncClassProcessor} 依此判断哪些类需要注入实例/静态跟踪调用。
 * 字段级的方向、编解码器等信息在运行时由
 * {@link dev.anvilcraft.lib.v2.sync.management.LazySyncManager}
 * 通过反射读取，故此处仅需记录类的集合。</p>
 */
@Slf4j
@ApiStatus.Internal
public class LazySyncTargetIndex {
    public static final String LAZY_SYNC_DESCRIPTOR = "Ldev/anvilcraft/lib/v2/sync/annotation/LazySync;";
    private static final Set<String> TARGETS = new HashSet<>();

    static {
        LazySyncTargetIndex.onLoad();
    }

    private LazySyncTargetIndex() {
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void onLoad() {
        int count = 0;
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                if (!annotation.annotationType().getDescriptor().equals(LAZY_SYNC_DESCRIPTOR)) continue;
                if (annotation.targetType() != ElementType.FIELD) continue;
                String internalName = annotation.clazz().getInternalName();
                if (TARGETS.add(internalName)) {
                    log.info("Registered @LazySync target: {}", internalName);
                }
                count++;
            }
        }
        log.info("Scan complete – {} @LazySync field(s) across {} class(es).", count, TARGETS.size());
    }

    public static boolean contains(String internalName) {
        return TARGETS.contains(internalName);
    }
}
