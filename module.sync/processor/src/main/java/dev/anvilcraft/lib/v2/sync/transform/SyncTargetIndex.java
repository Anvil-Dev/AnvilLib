package dev.anvilcraft.lib.v2.sync.transform;

import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SyncTargetIndex {
    public static final String SYNC_DESCRIPTOR = "Ldev/anvilcraft/lib/v2/sync/annotation/Sync;";
    private static final Map<String, String> TARGETS = new HashMap<>();

    static {
        SyncTargetIndex.onLoad();
    }

    private SyncTargetIndex() {
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void onLoad() {
        int count = 0;
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                if (!annotation.annotationType().getDescriptor().equals(SYNC_DESCRIPTOR)) continue;
                if (annotation.targetType() != ElementType.TYPE) continue;
                Object object = annotation.annotationData().get("value");
                String direction = "BOTH";
                if (object != null) {
                    direction = object.toString();
                }
                String internalName = annotation.clazz().getInternalName();
                SyncTargetIndex.add(internalName, direction);
                log.info("Registered @Sync target: {}", internalName);
                count++;
            }
        }
        log.info("Scan complete – {} @Sync target(s) registered.", count);
    }

    private static void add(String internalName, String direction) {
        TARGETS.put(internalName, direction);
    }

    public static boolean contains(String internalName) {
        boolean contains = TARGETS.containsKey(internalName);
        if (contains) {
            log.info("Checking if {} is a @Sync target", internalName);
        }
        return contains;
    }

    public static String getDirection(String internalName) {
        return TARGETS.get(internalName);
    }
}

