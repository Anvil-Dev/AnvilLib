package dev.anvilcraft.lib.v2.config.client;

import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.config.ConfigRecord;
import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTabsTest {
    @Test
    void generalComesFirstAndGroupsKeepSpecOrder() {
        var builder = new ModConfigSpec.Builder();
        builder.push("second").define("a", true);
        builder.pop().define("root", 12);
        builder.push("first").define("b", false);
        var tabs = ConfigTabs.of(builder.build());
        assertEquals(List.of("", "second", "first"), tabs.stream().map(ConfigTabs.Tab::key).toList());
        assertEquals(List.of("root"), tabs.getFirst().entries().stream().map(e -> e.getKey()).toList());
    }

    @Test
    void groupsOnlyDoNotCreateGeneral() {
        var builder = new ModConfigSpec.Builder();
        builder.push("group").define("enabled", true);
        assertEquals(List.of("group"), ConfigTabs.of(builder.build()).stream().map(ConfigTabs.Tab::key).toList());
    }

    @Test
    void emptySpecDoesNotInventAnEditableTab() {
        assertTrue(ConfigTabs.of(new ModConfigSpec.Builder().build()).isEmpty());
    }

    @Test
    void nestedSectionsStayInsideTheirParentTab() {
        var builder = new ModConfigSpec.Builder();
        builder.push("display").push("details").push("colors").define("blue", true);
        var tabs = ConfigTabs.of(builder.build());
        assertEquals(List.of("display"), tabs.stream().map(ConfigTabs.Tab::key).toList());
        assertEquals(List.of("details"), tabs.getFirst().entries().stream().map(e -> e.getKey()).toList());
    }

    @Test
    void annotationDefaultsRemainCompatible() {
        assertEquals(ConfigScreenStyle.DEFAULT, Legacy.class.getAnnotation(Config.class).screenStyle());
        assertEquals(ConfigScreenStyle.SIDEBAR, Example.class.getAnnotation(Config.class).screenStyle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fieldsWithoutCommentsStillHaveFullTranslationPaths() throws ReflectiveOperationException {
        var constructor = ConfigManager.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        ConfigManager manager = constructor.newInstance("test");
        Example object = manager.register(new Example());
        var field = ConfigManager.class.getDeclaredField("configSpecMap");
        field.setAccessible(true);
        ConfigRecord record = ((Map<Object, ConfigRecord>) field.get(manager)).get(object);
        ModConfigSpec.ValueSpec value = record.spec().getSpec().get(List.of("group", "value"));
        assertEquals("test.configuration.group.value", value.getTranslationKey());
        assertEquals("test.configuration.group", record.spec().getLevelTranslationKey(List.of("group")));
        assertEquals("folder/test-common.toml", record.getFileName());
    }

    @Config(name = "legacy")
    static class Legacy {}

    @Config(name = "test", group = "folder", screenStyle = ConfigScreenStyle.SIDEBAR)
    static class Example {
        @CollapsibleObject
        public Group group = new Group();
    }

    static class Group {
        public boolean value = true;
    }
}
