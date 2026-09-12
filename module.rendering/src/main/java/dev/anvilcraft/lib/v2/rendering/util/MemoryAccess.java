package dev.anvilcraft.lib.v2.rendering.util;

import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.Pointer;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.LongPredicate;

import static org.lwjgl.system.Pointer.BITS32;
import static org.lwjgl.system.jni.JNINativeInterface.NewDirectByteBuffer;

/// @author IMS212
@SuppressWarnings("removal")
@ApiStatus.Internal
public class MemoryAccess {
    private static final Unsafe UNSAFE = getUnsafe();
    private static final boolean BITS32 = Pointer.BITS32;

    private static final long ADDRESS = getAddressOffset();

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static long memAddress(ByteBuffer buffer) {
        return buffer.position() + UNSAFE.getLong(buffer, ADDRESS);
    }

    public static void memset(long address, long size, byte value) {
        UNSAFE.setMemory(address, size, value);
    }

    public static void putInt(long address, int value) {
        UNSAFE.putInt(address, value);
    }

    public static void putFloat(long address, float value) {
        UNSAFE.putFloat(address, value);
    }

    public static void putLong(long address, long value) {
        UNSAFE.putLong(address, value);
    }

    public static void putShort(long address, short value) {
        UNSAFE.putShort(address, value);
    }

    public static void putByte(long address, byte b) {
        UNSAFE.putByte(address, b);
    }

    public static int getInt(long address) {
        return UNSAFE.getInt(address);
    }

    public static float getFloat(long address) {
        return UNSAFE.getFloat(address);
    }

    public static long getLong(long address) {
        return UNSAFE.getLong(address);
    }

    public static short getShort(long address) {
        return UNSAFE.getShort(address);
    }

    public static byte getByte(long address) {
        return UNSAFE.getByte(address);
    }

    public static void putAddress(long address, long value) {
        if (BITS32) {
            UNSAFE.putInt(address, (int) value);
        } else {
            UNSAFE.putLong(address, value);
        }
    }

    public static long getAddress(long address) {
        if (BITS32) {
            return UNSAFE.getInt(address) & 0xFFFF_FFFFL;
        } else {
            return UNSAFE.getLong(address);
        }
    }

    private static long getFieldOffset(Class<?> containerType, Class<?> fieldType, LongPredicate predicate) {
        Class<?> c = containerType;
        while (c != Object.class) {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (!field.getType().isAssignableFrom(fieldType) || Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                long offset = UNSAFE.objectFieldOffset(field);
                if (predicate.test(offset)) {
                    return offset;
                }
            }
            c = c.getSuperclass();
        }
        throw new UnsupportedOperationException("Failed to find field offset in class.");
    }

    private static long getAddressOffset() {
        long MAGIC_ADDRESS = 0xDEADBEEF8BADF00DL & (BITS32 ? 0xFFFF_FFFFL : 0xFFFF_FFFF_FFFF_FFFFL);

        ByteBuffer bb = Objects.requireNonNull(NewDirectByteBuffer(MAGIC_ADDRESS, 0));

        return getFieldOffset(bb.getClass(), long.class, offset -> UNSAFE.getLong(bb, offset) == MAGIC_ADDRESS);
    }
}