package dev.anvilcraft.lib.v2.test;

import com.google.gson.annotations.SerializedName;
import dev.anvilcraft.lib.v2.config.CollapsibleObject;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.config.util.TranslatableEnum;

@Config(name = AnvilLibTest.MOD_ID, group = "anvillib")
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
        TEST3
    }
}
