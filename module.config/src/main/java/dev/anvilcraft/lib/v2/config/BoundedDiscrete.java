package dev.anvilcraft.lib.v2.config;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BoundedDiscrete {
    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;

    @ApiStatus.Internal
    class Util {
        public static int minInt(BoundedDiscrete annotation) {
            if (annotation.min() == Double.NEGATIVE_INFINITY) return Integer.MIN_VALUE;
            return (int) Math.clamp(Math.floor(annotation.min()), Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        public static int maxInt(BoundedDiscrete annotation) {
            if (annotation.max() == Double.POSITIVE_INFINITY) return Integer.MAX_VALUE;
            return (int) Math.clamp(Math.ceil(annotation.max()), Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        public static long minLong(BoundedDiscrete annotation) {
            if (annotation.min() == Double.NEGATIVE_INFINITY) return Long.MIN_VALUE;
            return (long) Math.clamp(Math.floor(annotation.min()), Long.MIN_VALUE, Long.MAX_VALUE);
        }

        public static long maxLong(BoundedDiscrete annotation) {
            if (annotation.max() == Double.POSITIVE_INFINITY) return Long.MAX_VALUE;
            return (long) Math.clamp(Math.ceil(annotation.max()), Long.MIN_VALUE, Long.MAX_VALUE);
        }

        public static float minFloat(BoundedDiscrete annotation) {
            if (annotation.min() == Double.NEGATIVE_INFINITY) return Float.NEGATIVE_INFINITY;
            return (float) Math.clamp(Math.floor(annotation.min()), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        public static float maxFloat(BoundedDiscrete annotation) {
            if (annotation.max() == Double.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY;
            return (float) Math.clamp(Math.ceil(annotation.max()), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        public static double minDouble(BoundedDiscrete annotation) {
            if (annotation.min() == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
            return Math.clamp(Math.floor(annotation.min()), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        public static double maxDouble(BoundedDiscrete annotation) {
            if (annotation.max() == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
            return Math.clamp(Math.ceil(annotation.max()), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        public static double minShort(BoundedDiscrete annotation) {
            if (annotation.min() == Short.MIN_VALUE) return Short.MIN_VALUE;
            return Math.clamp(Math.floor(annotation.min()), Short.MIN_VALUE, Short.MAX_VALUE);
        }

        public static double maxShort(BoundedDiscrete annotation) {
            if (annotation.max() == Short.MAX_VALUE) return Short.MAX_VALUE;
            return Math.clamp(Math.ceil(annotation.max()), Short.MIN_VALUE, Short.MAX_VALUE);
        }

        public static double minByte(BoundedDiscrete annotation) {
            if (annotation.min() == Byte.MIN_VALUE) return Byte.MIN_VALUE;
            return Math.clamp(Math.floor(annotation.min()), Byte.MIN_VALUE, Byte.MAX_VALUE);
        }

        public static double maxByte(BoundedDiscrete annotation) {
            if (annotation.max() == Byte.MAX_VALUE) return Byte.MAX_VALUE;
            return Math.clamp(Math.ceil(annotation.max()), Byte.MIN_VALUE, Byte.MAX_VALUE);
        }
    }
}
