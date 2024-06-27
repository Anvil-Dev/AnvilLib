package dev.anvilcraft.lib.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.anvilcraft.lib.AnvilLib;
import dev.anvilcraft.lib.integration.AnvilLibIntegrations;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mod(AnvilLib.MOD_ID)
public class AnvilLibForge {
    public AnvilLibForge() {
        AnvilLib.init();

        for (IModInfo mod : ModList.get().getMods()) {
            Map<String, Object> modProperties = mod.getModProperties();
            for (Map.Entry<String, Object> entry : modProperties.entrySet()) {
                if (!entry.getKey().equals("anvilcraft")) continue;
                if (!(entry.getValue() instanceof UnmodifiableConfig anvilConfig)) return;
                for (UnmodifiableConfig.Entry entry1 : anvilConfig.entrySet()) {
                    if (!entry1.getKey().equals("integrations")) continue;
                    if (!(entry1.getValue() instanceof UnmodifiableConfig config)) return;
                    AnvilLibForge.loadIntegrations(config);
                }
            }
        }
        AnvilLibIntegrations.apply();
    }

    private static void loadIntegrations(@NotNull UnmodifiableConfig integrations) {
        for (UnmodifiableConfig.Entry entry2 : integrations.entrySet()) {
            String modid = entry2.getKey();
            Object value = entry2.getValue();
            List<String> classes = Collections.synchronizedList(new ArrayList<>());
            if (value instanceof String string) {
                classes.add(string);
            } else if (value instanceof List<?> list) {
                list.stream()
                    .filter(i -> i instanceof String)
                    .map(Object::toString)
                    .forEach(classes::add);
            }
            AnvilLibIntegrations.INTEGRATIONS.put(modid, classes);
        }
    }
}