package dev.anvilcraft.lib.v2.sync.management;

import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.network.payload.SyncConfigurationPayload;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@ApiStatus.Internal
public class SyncConfigManager {
    public static final String SYNC_DESCRIPTOR = "Ldev/anvilcraft/lib/v2/sync/annotation/Sync;";
    public static final String LAZY_SYNC_DESCRIPTOR = "Ldev/anvilcraft/lib/v2/sync/annotation/LazySync;";
    private static final String SYNC_PROXY_DESC = "Ldev/anvilcraft/lib/v2/sync/management/SyncProxy;";
    public int usedId = 0;
    public final Map<String, Integer> syncConfigs = new HashMap<>();
    public final Map<Integer, String> syncConfigById = new HashMap<>();


    @ApiStatus.Internal
    @SneakyThrows
    @SuppressWarnings("UnstableApiUsage")
    public static void compileContent() {
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                if (annotation.annotationType().getDescriptor().equals(SyncConfigManager.SYNC_DESCRIPTOR)) {
                    if (annotation.targetType() != ElementType.TYPE) continue;
                    compileSyncType(fileInfo, annotation.clazz().getClassName());
                } else if (annotation.annotationType().getDescriptor().equals(SyncConfigManager.LAZY_SYNC_DESCRIPTOR)) {
                    if (annotation.targetType() != ElementType.FIELD) continue;
                    // FIELD 目标的 memberName 即字段名
                    String key = "%s#%s".formatted(annotation.clazz().getClassName(), annotation.memberName());
                    log.info("Loading LazySyncConfig: {}", key);
                    AnvilLibSync.SYNC_CONFIG_MANAGER.register(key);
                }
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void compileSyncType(ModFileInfo fileInfo, String className) {
        IModFile modFile = fileInfo.getFile();
        String classPath = className.replace('.', '/') + ".class";
        log.info("Loading SyncConfig: {}", className);
        JarContents modFileContents = modFile.getContents();
        if (modFileContents.containsFile(classPath)) {
            try (InputStream inputStream = modFileContents.openFile(classPath)) {
                if (inputStream != null) {
                    ClassReader classReader = new ClassReader(inputStream);
                    FieldListingVisitor fieldListingVisitor = new FieldListingVisitor(className, null);
                    classReader.accept(fieldListingVisitor, 0);
                }
            } catch (Exception e) {
                log.error("Error while reading class file: {}", classPath, e);
            }
        }
    }

    public static class FieldListingVisitor extends ClassVisitor {
        private final String className;

        FieldListingVisitor(String className, @Nullable ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);
            this.className = className;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (Objects.equals(descriptor, SyncConfigManager.SYNC_PROXY_DESC) && (access & Opcodes.ACC_FINAL) != 0) {
                AnvilLibSync.SYNC_CONFIG_MANAGER.register("%s#%s".formatted(this.className, name));
            }
            return super.visitField(access, name, descriptor, signature, value);
        }
    }

    public void register(String syncConfig) {
        this.syncConfigs.put(syncConfig, this.usedId);
        this.syncConfigById.put(this.usedId, syncConfig);
        this.usedId++;
    }

    public int getId(String syncConfig) {
        return this.syncConfigs.get(syncConfig);
    }

    public String getById(int id) {
        return this.syncConfigById.get(id);
    }

    public void clear() {
        this.syncConfigs.clear();
        this.syncConfigById.clear();
        this.usedId = 0;
    }


    public void registerAll(Map<Integer, String> syncConfigById) {
        this.clear();
        syncConfigById.forEach((id, syncConfig) -> {
            this.syncConfigs.put(syncConfig, id);
            this.syncConfigById.put(id, syncConfig);
            if (id >= this.usedId) {
                this.usedId = id + 1;
            }
        });
    }

    public SyncConfigurationPayload createPyload() {
        return new SyncConfigurationPayload(this.syncConfigById);
    }
}
