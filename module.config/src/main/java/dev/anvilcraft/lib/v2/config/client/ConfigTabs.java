package dev.anvilcraft.lib.v2.config.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 直接依据 spec 分组，不改变配置文件路径或字段顺序。 */
final class ConfigTabs {
    record Tab(String key, Set<? extends UnmodifiableConfig.Entry> entries, Map<String, Object> specs) {}

    static List<Tab> of(ModConfigSpec spec) {
        List<Tab> tabs = new ArrayList<>();
        Set<UnmodifiableConfig.Entry> general = new LinkedHashSet<>();
        Map<String, UnmodifiableConfig.Entry> values = new HashMap<>();
        spec.getValues().entrySet().forEach(entry -> values.put(entry.getKey(), entry));
        for (UnmodifiableConfig.Entry entry : spec.getSpec().entrySet()) {
            UnmodifiableConfig.Entry value = values.get(entry.getKey());
            if (value == null) continue;
            if (entry.getRawValue() instanceof UnmodifiableConfig section
                && value.getRawValue() instanceof UnmodifiableConfig sectionValues) {
                tabs.add(new Tab(entry.getKey(), sectionValues.entrySet(), section.valueMap()));
            } else {
                general.add(value);
            }
        }
        if (!general.isEmpty()) tabs.addFirst(new Tab("", general, spec.getSpec().valueMap()));
        return List.copyOf(tabs);
    }
}
