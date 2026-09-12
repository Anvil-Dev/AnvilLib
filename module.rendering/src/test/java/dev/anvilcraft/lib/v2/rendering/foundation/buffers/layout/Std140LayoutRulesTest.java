package dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std140.Std140SizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std140.Std140Writer;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std430.Std430SizeCalculator;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.std430.Std430Writer;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntryType;

import java.nio.ByteBuffer;

public final class Std140LayoutRulesTest {
    private static final BufferObjectLayoutEntryType<Object> TEST_VEC2 = new BufferObjectLayoutEntryType<>() {
        @Override
        public void acceptSizeCalculator(BufferSizeCalculator sizeCalculator) {
            sizeCalculator.putVec2();
        }

        @Override
        public void acceptWriter(BufferWriter writer, Object object) {
        }

        @Override
        public int alignment(BufferLayout layout) {
            return 8;
        }
    };

    private Std140LayoutRulesTest() {
    }

    public static void main(String[] args) {
        BufferObjectLayoutDefinition<TestStruct> structDefinition = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<Object, TestStruct>builder(TEST_VEC2).forGetter(ignored -> null).build(),
            BufferObjectLayoutEntry.<TestStruct>ofFloat().forGetter(TestStruct::value).build()
        );

        assertEquals(16, structDefinition.alignment(BufferLayout.STD140), "std140 struct alignment");

        Std140SizeCalculator calculator = new Std140SizeCalculator();
        calculator.putStructArray(structDefinition, 2);

        assertEquals(32, calculator.get(), "std140 struct array size");

        BufferObjectLayoutDefinition<TestStruct> compactStructDefinition = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<TestStruct>ofFloat().forGetter(TestStruct::value).build(),
            BufferObjectLayoutEntry.<TestStruct>ofFloat().forGetter(TestStruct::value).build()
        );

        Std430SizeCalculator std430Calculator = new Std430SizeCalculator();
        std430Calculator.putStructArray(compactStructDefinition, 2);

        assertEquals(16, std430Calculator.get(), "std430 struct array size");

        BufferObjectLayoutDefinition<TestPair> pairDefinition = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<TestPair>ofFloat().forGetter(TestPair::first).build(),
            BufferObjectLayoutEntry.<TestPair>ofFloat().forGetter(TestPair::second).build()
        );

        ByteBuffer std140Buffer = ByteBuffer.allocate(64);
        Std140Writer std140Writer = new Std140Writer(std140Buffer);
        std140Writer.putStructArray(new TestPair[]{
            new TestPair(1.0f, 2.0f),
            new TestPair(3.0f, 4.0f)
        }, pairDefinition);

        assertFloatEquals(1.0f, std140Buffer.getFloat(0), "std140 array[0].first");
        assertFloatEquals(2.0f, std140Buffer.getFloat(4), "std140 array[0].second");
        assertFloatEquals(3.0f, std140Buffer.getFloat(16), "std140 array[1].first");
        assertFloatEquals(4.0f, std140Buffer.getFloat(20), "std140 array[1].second");

        ByteBuffer std430Buffer = ByteBuffer.allocate(64);
        Std430Writer std430Writer = new Std430Writer(std430Buffer);
        std430Writer.putStructArray(new TestPair[]{
            new TestPair(1.0f, 2.0f),
            new TestPair(3.0f, 4.0f)
        }, pairDefinition);

        assertFloatEquals(1.0f, std430Buffer.getFloat(0), "std430 array[0].first");
        assertFloatEquals(2.0f, std430Buffer.getFloat(4), "std430 array[0].second");
        assertFloatEquals(3.0f, std430Buffer.getFloat(8), "std430 array[1].first");
        assertFloatEquals(4.0f, std430Buffer.getFloat(12), "std430 array[1].second");

        ByteBuffer std140IndexedBuffer = ByteBuffer.allocate(64);
        Std140Writer std140IndexedWriter = new Std140Writer(std140IndexedBuffer);
        std140IndexedWriter.putStructArray(1, new TestPair(5.0f, 6.0f), pairDefinition);

        assertFloatEquals(0.0f, std140IndexedBuffer.getFloat(0), "std140 indexed array[0].first");
        assertFloatEquals(5.0f, std140IndexedBuffer.getFloat(16), "std140 indexed array[1].first");
        assertFloatEquals(6.0f, std140IndexedBuffer.getFloat(20), "std140 indexed array[1].second");

        ByteBuffer std430IndexedBuffer = ByteBuffer.allocate(64);
        Std430Writer std430IndexedWriter = new Std430Writer(std430IndexedBuffer);
        std430IndexedWriter.putStructArray(1, new TestPair(5.0f, 6.0f), pairDefinition);

        assertFloatEquals(0.0f, std430IndexedBuffer.getFloat(0), "std430 indexed array[0].first");
        assertFloatEquals(5.0f, std430IndexedBuffer.getFloat(8), "std430 indexed array[1].first");
        assertFloatEquals(6.0f, std430IndexedBuffer.getFloat(12), "std430 indexed array[1].second");

        ByteBuffer std140SequentialIndexedBuffer = ByteBuffer.allocate(64);
        Std140Writer std140SequentialIndexedWriter = new Std140Writer(std140SequentialIndexedBuffer);
        std140SequentialIndexedWriter.putStructArray(0, new TestPair(1.0f, 2.0f), pairDefinition);
        std140SequentialIndexedWriter.putStructArray(1, new TestPair(3.0f, 4.0f), pairDefinition);

        assertFloatEquals(1.0f, std140SequentialIndexedBuffer.getFloat(0), "std140 sequential indexed array[0].first");
        assertFloatEquals(2.0f, std140SequentialIndexedBuffer.getFloat(4), "std140 sequential indexed array[0].second");
        assertFloatEquals(3.0f, std140SequentialIndexedBuffer.getFloat(16), "std140 sequential indexed array[1].first");
        assertFloatEquals(4.0f, std140SequentialIndexedBuffer.getFloat(20), "std140 sequential indexed array[1].second");

        ByteBuffer std430SequentialIndexedBuffer = ByteBuffer.allocate(64);
        Std430Writer std430SequentialIndexedWriter = new Std430Writer(std430SequentialIndexedBuffer);
        std430SequentialIndexedWriter.putStructArray(0, new TestPair(1.0f, 2.0f), pairDefinition);
        std430SequentialIndexedWriter.putStructArray(1, new TestPair(3.0f, 4.0f), pairDefinition);

        assertFloatEquals(1.0f, std430SequentialIndexedBuffer.getFloat(0), "std430 sequential indexed array[0].first");
        assertFloatEquals(2.0f, std430SequentialIndexedBuffer.getFloat(4), "std430 sequential indexed array[0].second");
        assertFloatEquals(3.0f, std430SequentialIndexedBuffer.getFloat(8), "std430 sequential indexed array[1].first");
        assertFloatEquals(4.0f, std430SequentialIndexedBuffer.getFloat(12), "std430 sequential indexed array[1].second");
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertFloatEquals(float expected, float actual, String label) {
        if (Float.compare(expected, actual) != 0) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private record TestStruct(float value) {
    }

    private record TestPair(float first, float second) {
    }
}
