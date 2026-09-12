package dev.anvilcraft.lib.v2.integration;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Optional;

@Slf4j
public class IntegrationManager {
    private final Multimap<String, IntegrationInstance> instances = MultimapBuilder.hashKeys().hashSetValues().build();

    public static final String INTEGRATION_NAME = "L" + Integration.class.getName().replace(".", "/") + ";";

    private final String modId;

    public IntegrationManager(String modId) {
        this.modId = modId;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void compileContent() {
        ModFileInfo fileInfo = FMLLoader.getCurrent().getLoadingModList().getModFileById(this.modId);
        ModFileScanData scanData = fileInfo.getFile().getScanResult();
        List<ModFileScanData.AnnotationData> list = scanData.getAnnotations()
            .stream()
            .filter(annotation -> annotation.annotationType()
                                      .getDescriptor()
                                      .equals(INTEGRATION_NAME) && annotation.targetType() == ElementType.TYPE)
            .toList();
        ProgressMeter meter = StartupNotificationManager.addProgressBar("Load Integrations", list.size());
        for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
            if (annotation.annotationType().getDescriptor().equals(INTEGRATION_NAME) && annotation.targetType() == ElementType.TYPE) {
                String modid = (String) annotation.annotationData().get("value");
                String version = (String) annotation.annotationData().get("version");
                //noinspection unchecked
                List<ModAnnotation.EnumHolder> typeHolders = ((List<ModAnnotation.EnumHolder>) annotation.annotationData().get("type"));
                if (version == null) version = "*";
                List<IntegrationType> type = List.of(IntegrationType.DEDICATED_SERVER, IntegrationType.CLIENT);
                if (typeHolders != null) {
                    type = typeHolders.stream().map(holder -> switch (holder.value()) {
                        case "DEDICATED_SERVER" -> IntegrationType.DEDICATED_SERVER;
                        case "CLIENT" -> IntegrationType.CLIENT;
                        case "CLIENT_DATA" -> IntegrationType.CLIENT_DATA;
                        case "SERVER_DATA" -> IntegrationType.SERVER_DATA;
                        default -> throw new IllegalArgumentException("Unknown integration type: " + holder.value());
                    }).toList();
                }
                log.info("Considering integration {} for {id:{}, version:{}}", annotation.memberName(), modid, version);
                IntegrationInstance instance = new IntegrationInstance(modid, ModVersionRange.of(version), annotation.memberName(), type);
                this.instances.put(modid, instance);
            }
            meter.increment();
        }
        StartupNotificationManager.popBar(meter);
    }

    public void load(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (FMLLoader.getCurrent().getDist().isDedicatedServer() && !instance.containsType(IntegrationType.DEDICATED_SERVER)) return;
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading integration {} for {}.", instance.getInstance(), modid);
            instance.invoke();
        }
    }

    public void loadClient(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading client integration {} for {}.", instance.getInstance(), modid);
            instance.invokeClient();
        }
    }

    public void loadClientData(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading client data integration {} for {}.", instance.getInstance(), modid);
            instance.invokeClientData();
        }
    }

    public void loadServerData(String modid, ModInfo info) {
        for (IntegrationInstance instance : instances.get(modid)) {
            if (!instance.is(info)) continue;
            instance.newInstance();
            log.info("Loading server data integration {} for {}.", instance.getInstance(), modid);
            instance.invokeServerData();
        }
    }

    public void loadAllIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = FMLLoader.getCurrent().getLoadingModList().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> load(key, modInfo));
        }
    }

    public void loadAllClientIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = FMLLoader.getCurrent().getLoadingModList().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> loadClient(key, modInfo));
        }
    }

    public void loadAllClientDataIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = FMLLoader.getCurrent().getLoadingModList().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> loadClientData(key, modInfo));
        }
    }

    public void loadAllServerDataIntegrations() {
        for (String key : instances.keySet()) {
            Optional<ModInfo> info = FMLLoader.getCurrent().getLoadingModList().getMods().stream().filter(it -> it.getModId().equals(key)).findFirst();
            info.ifPresent(modInfo -> loadServerData(key, modInfo));
        }
    }
}
