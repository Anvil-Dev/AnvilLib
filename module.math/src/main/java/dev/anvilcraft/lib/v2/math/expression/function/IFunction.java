package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;

import dev.anvilcraft.lib.v2.math.expression.NumberArguments;
import dev.anvilcraft.lib.v2.util.ISerializer;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 可被表达式调用的函数定义。
 *
 * <p>函数定义是一个类型化对象：{@link #DIRECT_CODEC} 按 {@link #type()} 分发到对应的
 * {@link Type}，因此下游只要注册一个新的 {@link Type} 就能定义自己的函数类型；具名函数则放在
 * {@link LibRegistries#FUNCTION_KEY} 这个数据包注册表里，表达式按名引用。</p>
 */
public interface IFunction {
    /**
     * 内联形式的编解码：按 {@link #type()} 分发。
     */
    Codec<IFunction> DIRECT_CODEC = Codec.lazyInitialized(
        () -> LibRegistries.FUNCTION_TYPE.byNameCodec().dispatch(IFunction::type, Type::codec)
    );
    /**
     * 引用 {@link LibRegistries#FUNCTION_KEY} 里条目的编解码，也用于内联定义。
     */
    Codec<Holder<IFunction>> HOLDER_CODEC = RegistryFileCodec.create(
        LibRegistries.FUNCTION_KEY,
        IFunction.DIRECT_CODEC
    );
    StreamCodec<RegistryFriendlyByteBuf, Holder<IFunction>> HOLDER_STREAM_CODEC = ByteBufCodecs
        .holderRegistry(LibRegistries.FUNCTION_KEY);
    StreamCodec<RegistryFriendlyByteBuf, IFunction> STREAM_CODEC = ByteBufCodecs
        .registry(LibRegistries.FUNCTION_TYPE_KEY)
        .dispatch(IFunction::type, Type::streamCodec);
    /**
     * 优先写入数据包注册表中已有的函数引用，找不到可引用的条目时退化为内联的函数定义。
     */
    Codec<IFunction> CODEC = Codec.of(
        new Codec<>() {
            @Override
            public <T> DataResult<Pair<IFunction, T>> decode(DynamicOps<T> ops, T input) {
                return IFunction.DIRECT_CODEC.parse(ops, input).map(function -> Pair.of(function, input));
            }

            @Override
            public <T> DataResult<T> encode(IFunction input, DynamicOps<T> ops, T prefix) {
                HolderGetter<IFunction> getter = IFunction.getter(ops);
                if (getter instanceof HolderLookup.RegistryLookup<IFunction> lookup) {
                    Optional<Holder.Reference<IFunction>> reference = lookup.listElements()
                        .filter(ref -> input.equals(ref.value()))
                        .findFirst();
                    if (reference.isPresent()) {
                        return IFunction.HOLDER_CODEC.encode(reference.get(), ops, prefix);
                    }
                }
                return IFunction.HOLDER_CODEC.encode(Holder.direct(input), ops, prefix);
            }
        },
        IFunction.HOLDER_CODEC.map(Holder::value)
    );

    /**
     * 从动态操作里取出函数注册表，拿不到时返回 {@code null}。
     */
    static @Nullable HolderGetter<IFunction> getter(DynamicOps<?> ops) {
        if (!(ops instanceof RegistryOps<?> registryOps)) return null;
        return registryOps.getter(LibRegistries.FUNCTION_KEY).orElse(null);
    }

    /**
     * 计算本次调用的结果。
     *
     * @param arguments 各参数表达式在调用点上下文中的求值结果，顺序与函数声明一致
     * @param inputs    调用点可访问的传入值，供表达式引用 {@code x}/{@code y}/{@code z} 与 {@code $(name)}
     */
    double apply(List<Double> arguments, NumberArguments inputs);

    Type<? extends IFunction> type();

    /**
     * 一种函数定义形式的编解码与网络编解码。
     *
     * @param <F> 这种形式对应的函数实现
     */
    interface Type<F extends IFunction> extends ISerializer<F>, StringRepresentable {
        MapCodec<F> codec();

        StreamCodec<RegistryFriendlyByteBuf, F> streamCodec();
    }
}
