package dev.anvilcraft.lib.v2.codec;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function13;
import com.mojang.datafixers.util.Function14;
import com.mojang.datafixers.util.Function15;
import com.mojang.datafixers.util.Function16;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 高频 {@link Codec} / {@link MapCodec} 场景的工具集合。
 *
 * <p>本类主要面向 Minecraft/NeoForge 常见数据结构：注册表对象、方块状态、
 * 可选字段以及枚举与字符串/整数之间的映射。
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class CodecUtil {
    /**
     * {@link Item} 的编解码器，使用物品注册表 id 字符串进行序列化。
     *
     * <p>当 id 不存在或解析结果为 {@link Items#AIR} 时解码失败。
     */
    public static final Codec<Item> ITEM = Codec.STRING.flatXmap(
        s -> {
            try {
                Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(s)).orElse(Items.AIR);
                if (item == Items.AIR) {
                    return DataResult.error(() -> "failed parse item key: " + s);
                } else {
                    return DataResult.success(item);
                }
            } catch (Exception e) {
                return DataResult.error(e::getMessage);
            }
        }, i -> {
            Identifier key = BuiltInRegistries.ITEM.getKey(i);
            if (key.equals(Identifier.parse("air"))) {
                return DataResult.error(() -> "failed parse item: " + i);
            } else {
                return DataResult.success(key.toString());
            }
        }
    );

    /**
     * {@link Block} 的编解码器，使用方块注册表 id 字符串进行序列化。
     *
     * <p>当 id 不存在或解析结果为 {@link Blocks#AIR} 时解码失败。
     */
    public static final Codec<Block> BLOCK = Codec.STRING.flatXmap(
        s -> {
            try {
                Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(s)).orElse(Blocks.AIR);
                if (block == Blocks.AIR) {
                    return DataResult.error(() -> "failed parse block key: " + s);
                } else {
                    return DataResult.success(block);
                }
            } catch (Exception e) {
                return DataResult.error(e::getMessage);
            }
        }, b -> {
            Identifier key = BuiltInRegistries.BLOCK.getKey(b);
            if (key.equals(Identifier.parse("air"))) {
                return DataResult.error(() -> "failed parse block: " + b);
            } else {
                return DataResult.success(key.toString());
            }
        }
    );

    /**
     * 数值提供器编解码器，既支持标准 provider 结构，也支持纯整数写法。
     *
     * <p>编码时，整数形式的 {@link ConstantValue} 会被压缩为整数字段。
     */
    public static final Codec<NumberProvider> NUMBER_PROVIDER = Codec.either(
        NumberProviders.CODEC,
        Codec.INT.xmap(ConstantValue::new, value -> Math.round(value.value()))
    ).xmap(
        Either::unwrap, provider -> {
            if (!(provider instanceof ConstantValue(float value)) || value - Math.floor(value) >= 1E-5) {
                return Either.left(provider);
            }
            return Either.right((ConstantValue) provider);
        }
    );
    /**
     * 带注册表校验的 {@link EntityType} 编解码器。
     */
    public static final Codec<EntityType<?>> ENTITY = Identifier.CODEC.flatXmap(
        id -> {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return DataResult.error(() -> "Could not find entity type " + id + " as it does not exist in ENTITY_TYPE registry.");
            }
            EntityType<?> e = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (e == null) {
                return DataResult.error(() -> "Could not find entity type " + id + " as it does not exist in ENTITY_TYPE registry.");
            }
            return DataResult.success(e);
        }, b -> {
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(b);
            if (!BuiltInRegistries.ENTITY_TYPE.containsValue(b)) {
                return DataResult.error(() -> "Could not find key of entity type " + key + " as it does not exist in ENTITY_TYPE registry.");
            } else {
                return DataResult.success(key);
            }
        }
    );
    /**
     * 字符编解码器，使用单字符字符串进行表示。
     */
    public static final Codec<Character> CHAR = Codec.STRING.flatXmap(
        s -> {
            if (s.length() != 1) {
                return DataResult.error(() -> "Expected single character string, got length " + s.length() + ": \"" + s + "\"");
            }
            return DataResult.success(s.charAt(0));
        },
        c -> DataResult.success(c.toString())
    );
    /**
     * 方块状态 MapCodec：通过 block id 分发，并可选携带 state 属性。
     */
    public static final MapCodec<BlockState> BLOCK_STATE_MAP_CODEC = BuiltInRegistries.BLOCK.byNameCodec().dispatchMap(
        "block", BlockBehaviour.BlockStateBase::getBlock, block -> {
            BlockState state = block.defaultBlockState();
            return CodecUtil.blockStatePropertiesCodec(state).codec().lenientOptionalFieldOf("state", state);
        }
    );

    /**
     * 将单条目 map 编解码器转换为直接值编解码器。
     *
     * @param mapCodec    源 map 编解码器（逻辑上包含一组 key/value）
     * @param keyGetter   编码时从目标对象提取 key
     * @param valueGetter 编码时从目标对象提取 value
     * @param factory     解码后根据 key/value 重建目标对象
     * @param <K>         map 的 key 类型
     * @param <V>         map 的 value 类型
     * @param <T>         目标对象类型
     * @return 转换后的编解码器
     */
    public static <K, V, T> Codec<T> byMap(
        Codec<Map<K, V>> mapCodec,
        Function<T, K> keyGetter,
        Function<T, V> valueGetter,
        BiFunction<K, V, T> factory
    ) {
        return mapCodec.flatXmap(
            map -> {
                if (map.size() != 1) {
                    return DataResult.error(() -> "Expected exactly one entry in map, got " + map.size());
                }
                Map.Entry<K, V> entry = map.entrySet().iterator().next();
                return DataResult.success(factory.apply(entry.getKey(), entry.getValue()));
            },
            value -> DataResult.success(Map.of(keyGetter.apply(value), valueGetter.apply(value)))
        );
    }

    /**
     * 基于列表序列化顺序创建 {@link LinkedList} 编解码器。
     *
     * @param codec 元素编解码器
     * @param <T>   元素类型
     * @return linked-list 编解码器
     */
    public static <T> Codec<LinkedList<T>> linkedListOf(Codec<T> codec) {
        return dequeOf(codec, LinkedList::new);
    }

    /**
     * 基于列表编解码器创建双端队列编解码器。
     *
     * @param codec    元素编解码器
     * @param dequeFac 根据列表快照构造 deque 的工厂
     * @param <T>      元素类型
     * @param <D>      deque 类型
     * @return deque 编解码器
     */
    public static <T, D extends Deque<T>> Codec<D> dequeOf(Codec<T> codec, Function<? super List<T>, ? extends D> dequeFac) {
        return codec.listOf().xmap(dequeFac, List::copyOf);
    }

    /**
     * 按枚举 ordinal 序号进行序列化的编解码器。
     *
     * @param clazz 枚举类
     * @param <T>   枚举类型
     * @return 基于 ordinal 的枚举编解码器
     */
    public static <T extends Enum<T>> Codec<T> enumCodecInInt(Class<T> clazz) {
        T[] constants = clazz.getEnumConstants();
        return Codec.INT.comapFlatMap(
            index -> {
                if (index < 0 || index >= constants.length) {
                    return DataResult.error(() ->
                        "Invalid ordinal " + index + " for enum " + clazz.getName()
                        + ", expected value in range [0, " + constants.length + ")"
                    );
                }
                return DataResult.success(constants[index]);
            }, Enum::ordinal
        );
    }

    /**
     * 按枚举名（小写）进行序列化的编解码器。
     *
     * @param clazz 枚举类
     * @param <T>   枚举类型
     * @return 小写名称枚举编解码器
     */
    public static <T extends Enum<T>> Codec<T> enumCodecInLowerName(Class<T> clazz) {
        return Codec.STRING.xmap(
            name -> Enum.valueOf(clazz, name.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT)
        );
    }

    /**
     * 构建带 {@code isPresent} 显式标记的 Optional 编解码器。
     *
     * <p>当协议要求显式存在位，而不是仅依赖字段缺失时，此方法更适用。
     *
     * @param elementCodec Optional 内部元素的编解码器
     * @param <T>          元素类型
     * @return 带显式存在标记的 Optional 编解码器
     */
    public static <T> Codec<Optional<T>> createOptionalCodec(Codec<T> elementCodec) {
        return CodecUtil.create(
            Codec.BOOL
                .fieldOf("isPresent")
                .forGetter(Optional::isPresent),
            elementCodec
                .optionalFieldOf("content")
                .forGetter(Function.identity()),
            (isPresent, content) -> isPresent && content.isPresent() ? content : Optional.empty()
        );
    }

    /**
     * 为配方类结构创建带数量上限的配料列表编解码器。
     *
     * @param fieldName  序列化字段名
     * @param size       允许的最大元素数量
     * @param recipeType 仅用于错误消息的配方类型名
     * @return 配料列表 MapCodec
     */
    public static MapCodec<NonNullList<Ingredient>> createIngredientListCodec(String fieldName, int size, String recipeType) {
        return Ingredient.CODEC.listOf(1, size).fieldOf(fieldName).flatXmap(
            i -> {
                Ingredient[] ingredients = i.toArray(Ingredient[]::new);
                if (ingredients.length == 0) {
                    return DataResult.error(() -> "No ingredients for %s recipe".formatted(recipeType));
                } else {
                    return ingredients.length > size
                           ? DataResult.error(() -> "Too many ingredients for %s recipe. The maximum is: %d".formatted(recipeType, size))
                           : DataResult.success(NonNullList.copyOf(List.of(ingredients)));
                }
            }, DataResult::success
        );
    }

    /**
     * 构建方块状态属性编解码器，将每个属性作为可选字段暴露。
     *
     * @param state 提供属性定义和默认值的原型状态
     * @param <T>   属性值类型
     * @return 该状态对应的属性 MapCodec
     */
    public static <T extends Comparable<T>> MapCodec<BlockState> blockStatePropertiesCodec(BlockState state) {
        AtomicReference<MapCodec<BlockState>> mapcodec = new AtomicReference<>(MapCodec.of(Encoder.empty(), Decoder.unit(state)));
        state.getValues().forEach((value) -> {
            Property<?> key = value.property();
            //noinspection unchecked
            mapcodec.set(CodecUtil.appendBlockStatePropertyCodec(
                mapcodec.get(),
                () -> state,
                key.getName(),
                (Property<T>) key,
                (Property.Value<T>) key.value(state)
            ));
        });
        return mapcodec.get();
    }

    /**
     * 向现有方块状态 MapCodec 追加一个属性字段编解码器。
     *
     * @param propertyCodec  现有 MapCodec 累加器
     * @param holderSupplier 用于回退默认值的状态提供器
     * @param value          序列化字段名
     * @param property       目标属性
     * @param defValue       默认属性值
     * @param <T>            属性可比较类型
     * @return 追加该属性后的 MapCodec
     */
    public static <T extends Comparable<T>> MapCodec<BlockState> appendBlockStatePropertyCodec(
        MapCodec<BlockState> propertyCodec,
        Supplier<BlockState> holderSupplier,
        String value,
        Property<T> property,
        Property.Value<T> defValue
    ) {
        return Codec.mapPair(
            propertyCodec, property.valueCodec().optionalFieldOf(value, defValue).orElseGet(
                key -> {
                }, () -> property.value(holderSupplier.get())
            )
        ).xmap(pair -> pair.getFirst().setValue(property, pair.getSecond().value()), state -> Pair.of(state, property.value(state)));
    }

    /// 构建一个 `ZOM列表`（零/一/多元素列表）MapCodec
    ///
    /// @param codec 元素编解码器
    /// @param name 名称
    /// @return `ZOM列表`（零/一/多元素列表）MapCodec
    public static <T> MapCodec<List<T>> zomListMap(Codec<T> codec, String name) {
        return Codec.mapEither(codec.optionalFieldOf(name), codec.listOf().fieldOf(name)).xmap(
            either -> either.map(
                op -> op
                    .map(Collections::singletonList)
                    .orElse(List.of()),
                Function.identity()
            ),
            list -> list.size() <= 1
                    ? list.isEmpty()
                      ? Either.left(Optional.empty())
                      : Either.left(Optional.of(list.getFirst()))
                    : Either.right(list)
        );
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R> 结果的类型
    /// @param <T1> 第一个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        Function<T1, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R> 结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        BiFunction<T1, T2, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        Function3<T1, T2, T3, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        Function4<T1, T2, T3, T4, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        Function5<T1, T2, T3, T4, T5, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        Function6<T1, T2, T3, T4, T5, T6, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    /// @param <T7> 第七个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        Function7<T1, T2, T3, T4, T5, T6, T7, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    /// @param <T7> 第七个参数的类型
    /// @param <T8> 第八个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    /// @param <T7> 第七个参数的类型
    /// @param <T8> 第八个参数的类型
    /// @param <T9> 第九个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param t14 第十四个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    /// @param <T14> 第十四个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        App<RecordCodecBuilder.Mu<R>, T14> t14,
        Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param t14 第十四个参数
    /// @param t15 第十五个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    /// @param <T14> 第十四个参数的类型
    /// @param <T15> 第十五个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        App<RecordCodecBuilder.Mu<R>, T14> t14,
        App<RecordCodecBuilder.Mu<R>, T15> t15,
        Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15).apply(inst, factory));
    }

    /// 创建 {@link Codec} 的便携方法，对 {@link RecordCodecBuilder#create(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param t14 第十四个参数
    /// @param t15 第十五个参数
    /// @param t16 第十六个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    /// @param <T14> 第十四个参数的类型
    /// @param <T15> 第十五个参数的类型
    /// @param <T16> 第十六个参数的类型
    ///
    /// @return 结果的 {@link Codec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Codec<R> create(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        App<RecordCodecBuilder.Mu<R>, T14> t14,
        App<RecordCodecBuilder.Mu<R>, T15> t15,
        App<RecordCodecBuilder.Mu<R>, T16> t16,
        Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> factory
    ) {
        return RecordCodecBuilder.create(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R> 结果的类型
    /// @param <T1> 第一个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        Function<T1, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R> 结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        BiFunction<T1, T2, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        Function3<T1, T2, T3, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        Function4<T1, T2, T3, T4, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        Function5<T1, T2, T3, T4, T5, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        Function6<T1, T2, T3, T4, T5, T6, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    /// @param <T7> 第七个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        Function7<T1, T2, T3, T4, T5, T6, T7, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    /// @param <T7> 第七个参数的类型
    /// @param <T8> 第八个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>  结果的类型
    /// @param <T1> 第一个参数的类型
    /// @param <T2> 第二个参数的类型
    /// @param <T3> 第三个参数的类型
    /// @param <T4> 第四个参数的类型
    /// @param <T5> 第五个参数的类型
    /// @param <T6> 第六个参数的类型
    /// @param <T7> 第七个参数的类型
    /// @param <T8> 第八个参数的类型
    /// @param <T9> 第九个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param t14 第十四个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    /// @param <T14> 第十四个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        App<RecordCodecBuilder.Mu<R>, T14> t14,
        Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param t14 第十四个参数
    /// @param t15 第十五个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    /// @param <T14> 第十四个参数的类型
    /// @param <T15> 第十五个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        App<RecordCodecBuilder.Mu<R>, T14> t14,
        App<RecordCodecBuilder.Mu<R>, T15> t15,
        Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15).apply(inst, factory));
    }

    /// 创建 {@link MapCodec} 的便携方法，对 {@link RecordCodecBuilder#mapCodec(Function)} 的代理
    ///
    /// <p>参考了 {@link net.minecraft.network.codec.StreamCodec#composite(StreamCodec, Function, Function)} 的设计</p>
    ///
    /// @param t1 第一个参数
    /// @param t2 第二个参数
    /// @param t3 第三个参数
    /// @param t4 第四个参数
    /// @param t5 第五个参数
    /// @param t6 第六个参数
    /// @param t7 第七个参数
    /// @param t8 第八个参数
    /// @param t9 第九个参数
    /// @param t10 第十个参数
    /// @param t11 第十一个参数
    /// @param t12 第十二个参数
    /// @param t13 第十三个参数
    /// @param t14 第十四个参数
    /// @param t15 第十五个参数
    /// @param t16 第十六个参数
    /// @param factory 使用所有参数创建结果的工厂
    ///
    /// @param <R>   结果的类型
    /// @param <T1>  第一个参数的类型
    /// @param <T2>  第二个参数的类型
    /// @param <T3>  第三个参数的类型
    /// @param <T4>  第四个参数的类型
    /// @param <T5>  第五个参数的类型
    /// @param <T6>  第六个参数的类型
    /// @param <T7>  第七个参数的类型
    /// @param <T8>  第八个参数的类型
    /// @param <T9>  第九个参数的类型
    /// @param <T10> 第十个参数的类型
    /// @param <T11> 第十一个参数的类型
    /// @param <T12> 第十二个参数的类型
    /// @param <T13> 第十三个参数的类型
    /// @param <T14> 第十四个参数的类型
    /// @param <T15> 第十五个参数的类型
    /// @param <T16> 第十六个参数的类型
    ///
    /// @return 结果的 {@link MapCodec}
    public static <R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> MapCodec<R> mapCodec(
        App<RecordCodecBuilder.Mu<R>, T1> t1,
        App<RecordCodecBuilder.Mu<R>, T2> t2,
        App<RecordCodecBuilder.Mu<R>, T3> t3,
        App<RecordCodecBuilder.Mu<R>, T4> t4,
        App<RecordCodecBuilder.Mu<R>, T5> t5,
        App<RecordCodecBuilder.Mu<R>, T6> t6,
        App<RecordCodecBuilder.Mu<R>, T7> t7,
        App<RecordCodecBuilder.Mu<R>, T8> t8,
        App<RecordCodecBuilder.Mu<R>, T9> t9,
        App<RecordCodecBuilder.Mu<R>, T10> t10,
        App<RecordCodecBuilder.Mu<R>, T11> t11,
        App<RecordCodecBuilder.Mu<R>, T12> t12,
        App<RecordCodecBuilder.Mu<R>, T13> t13,
        App<RecordCodecBuilder.Mu<R>, T14> t14,
        App<RecordCodecBuilder.Mu<R>, T15> t15,
        App<RecordCodecBuilder.Mu<R>, T16> t16,
        Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> factory
    ) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16).apply(inst, factory));
    }
}
