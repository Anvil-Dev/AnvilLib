package dev.anvilcraft.lib.v2.util;

import lombok.experimental.UtilityClass;
import net.minecraft.world.level.storage.loot.providers.number.ints.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.FromInt;

@UtilityClass
public final class NumberProviderUtil {
    public static double expected(ContextIntProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uni -> (expected(uni.min().value()) + expected(uni.max().value())) / 2;
            case BinomialDistributionGenerator binomial -> expected(binomial.n().value()) * expected(binomial.p().value());
            default -> -1;
        };
    }

    public static double expected(ContextFloatProvider numberProvider) {
        return switch (numberProvider) {
            case net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue value -> value.value();
            case net.minecraft.world.level.storage.loot.providers.number.floats.UniformGenerator uni ->
                (expected(uni.min().value()) + expected(uni.max().value())) / 2;
            case FromInt fromInt -> expected(fromInt.input().value());
            default -> -1;
        };
    }

    public static double max(ContextIntProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uni -> max(uni.max().value());
            case BinomialDistributionGenerator binomial -> max(binomial.n().value()) * max(binomial.p().value());
            default -> -1;
        };
    }

    public static double max(ContextFloatProvider numberProvider) {
        return switch (numberProvider) {
            case net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue value -> value.value();
            case net.minecraft.world.level.storage.loot.providers.number.floats.UniformGenerator uni -> max(uni.max().value());
            case FromInt fromInt -> max(fromInt.input().value());
            default -> -1;
        };
    }

    public static double min(ContextIntProvider numberProvider) {
        return switch (numberProvider) {
            case ConstantValue value -> value.value();
            case UniformGenerator uni -> min(uni.min().value());
            case BinomialDistributionGenerator binomial -> min(binomial.n().value()) * min(binomial.p().value());
            default -> -1;
        };
    }

    public static double min(ContextFloatProvider numberProvider) {
        return switch (numberProvider) {
            case net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue value -> value.value();
            case net.minecraft.world.level.storage.loot.providers.number.floats.UniformGenerator uni -> min(uni.min().value());
            case FromInt fromInt -> min(fromInt.input().value());
            default -> -1;
        };
    }
}
