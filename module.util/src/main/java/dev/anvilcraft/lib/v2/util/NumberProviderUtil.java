package dev.anvilcraft.lib.v2.util;

import lombok.experimental.UtilityClass;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

@UtilityClass
public final class NumberProviderUtil {
    public static double expected(NumberProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uni -> (expected(uni.min()) + expected(uni.max())) / 2;
            case BinomialDistributionGenerator binomial -> expected(binomial.n()) * expected(binomial.p());
            default -> -1;
        };
    }

    public static double max(NumberProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uni -> max(uni.max());
            case BinomialDistributionGenerator binomial -> max(binomial.n()) * max(binomial.p());
            default -> -1;
        };
    }

    public static double min(NumberProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uni -> min(uni.min());
            case BinomialDistributionGenerator binomial -> min(binomial.n()) * min(binomial.p());
            default -> -1;
        };
    }
}
