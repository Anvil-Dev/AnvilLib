package dev.anvilcraft.lib.v2.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;

@ApiStatus.Internal
public record ConfigField(
    Object object,
    Field field,
    ModConfigSpec.ConfigValue<?> value
) {
    public void load() {
        boolean isFinal = this.field.accessFlags().contains(AccessFlag.FINAL);
        boolean isStatic = this.field.accessFlags().contains(AccessFlag.STATIC);
        if (isFinal) return;
        boolean isPublic = this.field.accessFlags().contains(AccessFlag.PUBLIC);
        if (!isPublic) {
            this.field.setAccessible(true);
        }
        try {
            if (isStatic) {
                ConfigField.cast(null, this.field, this.value.get());
            } else {
                ConfigField.cast(this.object, this.field, this.value.get());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (!isPublic) {
                this.field.setAccessible(false);
            }
        }
    }

    static void cast(@Nullable Object obj, Field field, Object object) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.setBoolean(obj, (boolean) object);
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            long value = ConfigField.castToLong(object);
            field.setByte(obj, (byte) value);
        } else if (fieldType == short.class || fieldType == Short.class) {
            long value = ConfigField.castToLong(object);
            field.setShort(obj, (short) value);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            long value = ConfigField.castToLong(object);
            field.setInt(obj, (int) value);
        } else if (fieldType == long.class || fieldType == Long.class) {
            long value = ConfigField.castToLong(object);
            field.setLong(obj, value);
        } else if (fieldType == float.class || fieldType == Float.class) {
            double value = ConfigField.castToDouble(object);
            field.setFloat(obj, (float) value);
        } else if (fieldType == double.class || fieldType == Double.class) {
            double value = ConfigField.castToDouble(object);
            field.setDouble(obj, value);
        } else if (fieldType == char.class || fieldType == Character.class) {
            char value = (char) object;
            field.setChar(obj, (char) value);
        } else if (fieldType == String.class) {
            field.set(obj, (String) object);
        } else {
            field.set(obj, object);
        }
    }

    public static long castToLong(Object object) {
        if (object instanceof Number number) {
            return number.longValue();
        } else {
            return (long) object;
        }
    }

    public static double castToDouble(Object object) {
        if (object instanceof Number number) {
            return number.doubleValue();
        } else {
            return (double) object;
        }
    }
}
