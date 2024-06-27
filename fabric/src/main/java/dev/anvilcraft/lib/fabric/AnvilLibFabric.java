package dev.anvilcraft.lib.fabric;

import dev.anvilcraft.lib.AnvilLib;
import dev.anvilcraft.lib.event.fabric.ServerLifecycleEventListener;
import dev.anvilcraft.lib.integration.AnvilLibIntegrations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnvilLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AnvilLib.init();
        ServerLifecycleEventListener.init();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            CustomValue thisMod = mod.getMetadata().getCustomValue(AnvilLib.MOD_ID);
            if (thisMod == null || thisMod.getType() != CustomValue.CvType.OBJECT) continue;
            CustomValue.CvObject object = thisMod.getAsObject();
            if (object.containsKey("integrations")) {
                CustomValue integrations = object.get("integrations");
                if (integrations.getType() == CustomValue.CvType.OBJECT) {
                    AnvilLibFabric.loadIntegrations(integrations.getAsObject());
                }
            }
        }
        AnvilLibIntegrations.apply();
    }

    private static void loadIntegrations(@NotNull CustomValue.CvObject integrations) {
        for (Map.Entry<String, CustomValue> entry : integrations) {
            String modid = entry.getKey();
            CustomValue value = entry.getValue();
            List<String> classes = Collections.synchronizedList(new ArrayList<>());
            if (value.getType() == CustomValue.CvType.ARRAY) {
                for (CustomValue listValue : value.getAsArray()) {
                    if (listValue.getType() != CustomValue.CvType.STRING) continue;
                    classes.add(listValue.getAsString());
                }
            } else if (value.getType() == CustomValue.CvType.STRING) {
                classes.add(value.getAsString());
            }
            AnvilLibIntegrations.INTEGRATIONS.put(modid, classes);
        }
    }
}