package dev.anvilcraft.lib.v2.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SliderTest {
    @Test
    void decimalStepsRetainExactEndpointsAndClamp() {
        Slider.Range range = new Slider.Range(0.5, 4.0, 0.05);
        assertEquals(70, range.steps());
        assertEquals(0.5, range.value(-1));
        assertEquals(4.0, range.value(71));
        assertEquals(0.8, range.value(6));
        assertEquals(0, range.index(-10));
        assertEquals(70, range.index(10));
        assertEquals(6, range.index(0.8));
        assertEquals(7, range.index(0.825));
    }

    @Test
    void lastPartialStepCanReachMaximum() {
        Slider.Range range = new Slider.Range(-1, 1, 0.3);
        assertEquals(7, range.steps());
        assertEquals(0.8, range.value(6));
        assertEquals(1.0, range.value(7));
        assertEquals(7, range.index(0.99));
        assertEquals(6, range.index(0.85));
    }

    @Test
    void rejectsUnboundedOrInvalidMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new Slider.Range(0, Double.POSITIVE_INFINITY, 1));
        assertThrows(IllegalArgumentException.class, () -> new Slider.Range(1, 1, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new Slider.Range(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Slider.Range(0, 1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Slider.Range(0, 1, 1e-20));
    }

    @Test
    void attachesMetadataWithoutChangingNumericConfigValues() throws ReflectiveOperationException {
        var object = new Fields();
        var builder = new ModConfigSpec.Builder();
        var value = ConfigManager.define(builder, "zoom", Fields.class.getDeclaredField("zoom"), object.zoom);
        var spec = builder.build();
        assertInstanceOf(ModConfigSpec.DoubleValue.class, value);
        assertEquals(new Slider.Range(0.5, 4, 0.05), ConfigManager.slider(value));
        assertEquals(1.37, value.getDefault(), "Existing values are not rounded merely by registering a slider");
        ModConfigSpec.ValueSpec zoom = spec.getSpec().get("zoom");
        assertTrue(zoom.test(1.37));
        assertTrue(zoom.test(0.5));
        assertFalse(zoom.test(0.49));
        assertFalse(zoom.test(4.01));
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.define(new ModConfigSpec.Builder(),
            "unbounded", Fields.class.getDeclaredField("unbounded"), object.unbounded));
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.define(new ModConfigSpec.Builder(),
            "integer", Fields.class.getDeclaredField("integer"), object.integer));
    }

    @Test
    void fractionalFloatBoundsUseTheNormalizedTomlValue() throws ReflectiveOperationException {
        var builder = new ModConfigSpec.Builder();
        var object = new Fields();
        var value = ConfigManager.define(builder, "fraction", Fields.class.getDeclaredField("fraction"), object.fraction);
        var spec = builder.build();
        ModConfigSpec.ValueSpec fraction = spec.getSpec().get("fraction");
        assertEquals(0.1, value.getDefault());
        assertTrue(fraction.test(0.1));
        assertTrue(fraction.test(0.9));
        assertFalse(fraction.test(0.09));
        assertFalse(fraction.test(0.91));
    }

    static class Fields {
        @BoundedDiscrete(min = 0.5, max = 4)
        @Slider(step = 0.05)
        double zoom = 1.37;
        @BoundedDiscrete(min = 0.1, max = 0.9)
        @Slider(step = 0.1)
        float fraction = 0.1f;
        @Slider(step = 0.1)
        double unbounded = 1;
        @BoundedDiscrete(min = 0, max = 10)
        @Slider(step = 1)
        int integer = 1;
    }
}
