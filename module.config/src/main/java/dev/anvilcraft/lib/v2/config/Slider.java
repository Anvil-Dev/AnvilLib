package dev.anvilcraft.lib.v2.config;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Displays a float or double field as a stepped slider in the sidebar UI.
 * Requires a finite {@link BoundedDiscrete} range; values keep their original TOML type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Slider {
    double step();

    @ApiStatus.Internal
    record Range(double min, double max, double step) {
        public Range {
            if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max
                || !Double.isFinite(step) || step <= 0) {
                throw new IllegalArgumentException("Slider requires finite min < max and a positive finite step");
            }
            BigDecimal count = BigDecimal.valueOf(max).subtract(BigDecimal.valueOf(min))
                .divide(BigDecimal.valueOf(step), 0, RoundingMode.CEILING);
            if (count.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE - 1L)) > 0) {
                throw new IllegalArgumentException("Slider has too many steps");
            }
        }

        public int steps() {
            return BigDecimal.valueOf(max).subtract(BigDecimal.valueOf(min))
                .divide(BigDecimal.valueOf(step), 0, RoundingMode.CEILING).intValueExact();
        }

        public double value(int index) {
            if (index <= 0) return min;
            if (index >= steps()) return max;
            return Math.min(max, BigDecimal.valueOf(min)
                .add(BigDecimal.valueOf(step).multiply(BigDecimal.valueOf(index))).doubleValue());
        }

        public int index(double value) {
            if (Double.isNaN(value) || value <= min) return 0;
            int last = steps();
            if (value >= max || value > value(last - 1) / 2 + max / 2) return last;
            return Math.clamp(BigDecimal.valueOf(value).subtract(BigDecimal.valueOf(min))
                .divide(BigDecimal.valueOf(step), 0, RoundingMode.HALF_UP).longValue(), 0, last);
        }
    }
}
