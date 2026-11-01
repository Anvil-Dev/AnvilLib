package dev.anvilcraft.lib.v2.test;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.ConfigScreenStyle;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.util.TranslatableEnum;

@Config(name = AnvilLibTest.MOD_ID, group = "anvillib", screenStyle = ConfigScreenStyle.SIDEBAR)
public class AnvilLibTestConfig {
    public String testString = "test";
    public int testInt = 42;
    public boolean testBoolean = true;
    public double testDouble = 3.14;
    public float testFloat = 3.14f;
    public long testLong = 42L;
    public short testShort = 42;
    public byte testByte = 42;
    @CollapsibleObject
    public TestObject testObject = new TestObject();
    public TestEnum testEnum = TestEnum.TEST;
    @SerializedName("testString2testString2")
    @Comment("testString2testString2testString2")
    public String testString2 = "test2";

    @CollapsibleObject
    public Display display = new Display();
    @CollapsibleObject
    public TestObject performance = new TestObject();
    @CollapsibleObject
    public TestObject accessibility = new TestObject();
    @CollapsibleObject
    public TestObject notifications = new TestObject();
    @CollapsibleObject
    public TestObject particles = new TestObject();
    @CollapsibleObject
    public TestObject audio = new TestObject();
    @CollapsibleObject
    public TestObject interfaceOptions = new TestObject();
    @CollapsibleObject
    public TestObject advanced = new TestObject();

    public static class Display {
        @Comment("Show the configuration preview overlay.")
        public boolean enabled = true;
        @BoundedDiscrete(min = 0, max = 100)
        public int opacity = 75;
        @CollapsibleObject
        public Details details = new Details();
    }

    public static class Details {
        public String label = "Nested configuration";
        @CollapsibleObject
        public Colors colors = new Colors();
    }

    public static class Colors {
        public boolean highContrast = false;
        public TestEnum mode = TestEnum.TEST;
        @SerializedName("A deliberately long option label for checking clipping and tooltips")
        @Comment("Long labels must stay within the content panel at every GUI scale.")
        public String longLabel = "blue";
    }

    public static class TestObject {
        public String testString = "test";
        public int testInt = 42;
        public boolean testBoolean = true;
        public double testDouble = 3.14;
        public float testFloat = 3.14f;
        public long testLong = 42L;
        public short testShort = 42;
        public byte testByte = 42;
    }

    public enum TestEnum implements TranslatableEnum {
        @SerializedName("test")
        TEST,
        TEST2,
        TEST3,
        TEST4,
        TEST5,
        TEST6,
        TEST7,
        TEST8,
        TEST9,
        TEST10,
        TEST11,
        TEST12
    }
}
