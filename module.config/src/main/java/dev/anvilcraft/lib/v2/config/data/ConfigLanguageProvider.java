package dev.anvilcraft.lib.v2.config.data;

import net.minecraft.data.PackOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Config 模块自己的界面文本，无需使用方重复生成。 */
@EventBusSubscriber(modid = "anvillib_config", value = Dist.CLIENT)
public final class ConfigLanguageProvider extends LanguageProvider {
    private final boolean chinese;

    public ConfigLanguageProvider(PackOutput output, String locale) {
        super(output, "anvillib_config", locale);
        chinese = locale.equals("zh_cn");
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new ConfigLanguageProvider(event.getGenerator().getPackOutput(), "en_us"));
        event.addProvider(new ConfigLanguageProvider(event.getGenerator().getPackOutput(), "zh_cn"));
    }

    @Override
    protected void addTranslations() {
        add("anvillib_config.ui.general", chinese ? "常规" : "General");
        add("anvillib_config.ui.open", chinese ? "编辑 ›" : "Edit ›");
        add("anvillib_config.ui.back", chinese ? "返回" : "Back");
        add("anvillib_config.ui.empty", chinese ? "此配置没有可编辑的项目" : "This configuration has no editable entries");
    }
}
