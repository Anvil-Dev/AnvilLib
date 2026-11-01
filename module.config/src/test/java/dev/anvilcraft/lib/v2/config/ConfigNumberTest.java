package dev.anvilcraft.lib.v2.config;

import com.electronwill.nightconfig.toml.TomlFormat;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ConfigNumberTest {
    @Test
    void allNumericFieldsRemainValidAfterTomlRoundTrip() throws IllegalAccessException {
        Numbers object = new Numbers();
        var builder = new ModConfigSpec.Builder();
        for (Field field : Numbers.class.getDeclaredFields()) {
            ConfigManager.define(builder, field.getName(), field, field.get(object));
        }
        var spec = builder.build();
        var config = TomlFormat.newConfig();
        spec.correct(config);
        StringWriter writer = new StringWriter();
        TomlFormat.instance().createWriter().write(config, writer);
        var parsed = TomlFormat.instance().createParser().parse(writer.toString());
        assertTrue(spec.isCorrect(parsed), "Writing and reading a valid configuration must not trigger correction");
        assertEquals(7, ((Number) parsed.get("small")).intValue());
        assertEquals(42, ((Number) parsed.get("big")).longValue());
        assertEquals(3.14f, ((Number) parsed.get("fraction")).floatValue());
    }

    @Test
    void narrowTypesKeepTheirRanges() throws NoSuchFieldException {
        var builder = new ModConfigSpec.Builder();
        var object = new Numbers();
        ConfigManager.define(builder, "small", Numbers.class.getDeclaredField("small"), object.small);
        ConfigManager.define(builder, "bounded", Numbers.class.getDeclaredField("bounded"), object.bounded);
        var spec = builder.build();
        ModConfigSpec.ValueSpec small = spec.getSpec().get("small");
        ModConfigSpec.ValueSpec bounded = spec.getSpec().get("bounded");
        assertInstanceOf(ModConfigSpec.IntValue.class, spec.getValues().get("small"));
        assertInstanceOf(ModConfigSpec.IntValue.class, spec.getValues().get("bounded"));
        assertInstanceOf(Integer.class, small.getDefault());
        assertInstanceOf(Integer.class, bounded.getDefault());
        assertTrue(small.test(127));
        assertFalse(small.test(128));
        assertTrue(bounded.test(5));
        assertFalse(bounded.test(6));
    }

    static class Numbers {
        public byte small = 7;
        public short medium = 23;
        public int count = 42;
        public long big = 42L;
        public float fraction = 3.14f;
        public double precise = 3.14;
        @BoundedDiscrete(min = 1, max = 5)
        public short bounded = 2;
    }
}
