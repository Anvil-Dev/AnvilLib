package dev.anvilcraft.lib.v2.codec;

import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function13;
import com.mojang.datafixers.util.Function14;
import com.mojang.datafixers.util.Function15;
import com.mojang.datafixers.util.Function16;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.DistancePredicate;
import net.minecraft.advancements.criterion.EntityEquipmentPredicate;
import net.minecraft.advancements.criterion.EntityFlagsPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.advancements.criterion.FluidPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.LightPredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.advancements.criterion.MovementPredicate;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.advancements.criterion.SlotsPredicate;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.function.Function;

/**
 * 常见游戏领域对象与高阶组合编码的 {@link StreamCodec} 工具集合。
 *
 * <p>本类聚焦于网络包序列化的易用性：注册表对象编解码、NBT 桥接、枚举编码，
 * 以及面向不可变对象的高参数位数 composite 重载。
 */
@SuppressWarnings(
    value = {
        "DuplicatedCode",
        "unused"
    }
)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class StreamCodecUtil {
    /**
     * {@link Item} 的编解码器，按注册表 key 字符串编码。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM = StreamCodec.of(
        (buf, item) -> buf.writeIdentifier(BuiltInRegistries.ITEM.getKey(item)),
        buf -> {
            Identifier id = buf.readIdentifier();
            return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item id: " + id));
        }
    );
    /**
     * {@link Block} 的编解码器，按注册表 key 字符串编码。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Block> BLOCK = StreamCodec.of(
        (buf, block) -> buf.writeIdentifier(BuiltInRegistries.BLOCK.getKey(block)),
        buf -> {
            Identifier id = buf.readIdentifier();
            return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown block id: " + id));
        }
    );
    /**
     * {@link BlockPos} 的编解码器，使用 {@link net.minecraft.network.VarInt} 打包。
     */
    public static final StreamCodec<FriendlyByteBuf, BlockPos> VAR_INT_BLOCK_POS = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        BlockPos::getX,
        ByteBufCodecs.VAR_INT,
        BlockPos::getY,
        ByteBufCodecs.VAR_INT,
        BlockPos::getZ,
        BlockPos::new
    );
    /**
     * {@link BlockState} 的编解码器，按全局运行时 state id 编码。
     */
    public static final StreamCodec<? super ByteBuf, BlockState> BLOCK_STATE = StreamCodec.of(
        (buf, blockState) -> buf.writeInt(Block.getId(blockState)), (buf) -> Block.stateById(buf.readInt()));
    /**
     * {@link EntityType} 的编解码器，按资源定位符编码。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityType<?>> ENTITY = StreamCodec.of(
        (buf, e) -> buf.writeIdentifier(BuiltInRegistries.ENTITY_TYPE.getKey(e)),
        buf -> BuiltInRegistries.ENTITY_TYPE.getValue(buf.readIdentifier())
    );
    /**
     * 单个 {@link Character} 的编解码器，使用单字符 UTF 字符串表示。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Character> CHAR = StreamCodec.of(
        (buf, character) -> buf.writeUtf(character.toString()),
        buf -> {
            String value = buf.readUtf(1);
            if (value.length() != 1) {
                throw new IllegalArgumentException("Expected exactly one character but got length " + value.length());
            }
            return value.charAt(0);
        }
    );
    /**
     * 对原生 Vec3 读写方法的轻量封装。
     */
    public static final StreamCodec<FriendlyByteBuf, Vec3> VEC3 = StreamCodec.of(
        (buf, vec3) -> {
            buf.writeFloat((float) vec3.x);
            buf.writeFloat((float) vec3.y);
            buf.writeFloat((float) vec3.z);
        },
        (buf) -> {
            float x = buf.readFloat();
            float y = buf.readFloat();
            float z = buf.readFloat();
            return new Vec3(x, y, z);
        }
    );

    /**
     * {@link Vec3i} 编解码器，使用 {@link BlockPos#asLong(int, int, int)} 打包。
     */
    public static final StreamCodec<ByteBuf, Vec3i> VEC3I = new StreamCodec<>() {
        @Override
        public Vec3i decode(ByteBuf buffer) {
            // 保持与 BlockPos packed long 一致的线协议格式，避免兼容性问题。
            long packedPos = buffer.readLong();
            return new Vec3i(BlockPos.getX(packedPos), BlockPos.getY(packedPos), BlockPos.getZ(packedPos));
        }

        @Override
        public void encode(ByteBuf buffer, Vec3i value) {
            buffer.writeLong(BlockPos.asLong(value.getX(), value.getY(), value.getZ()));
        }
    };
    /**
     * 紧凑 number-provider 编码中 {@link ConstantValue} 的类型标记。
     */
    private static final byte CONSTANT_TYPE = 1;
    /**
     * 紧凑 number-provider 编码中 {@link UniformGenerator} 的类型标记。
     */
    private static final byte UNIFORM_TYPE = 2;
    /**
     * 紧凑 number-provider 编码中 {@link BinomialDistributionGenerator} 的类型标记。
     */
    private static final byte BINOMIAL_TYPE = 3;
    /**
     * 不支持的 provider 类型写入的保底标记。
     */
    private static final byte UNKNOWN_TYPE = -1;
    /**
     * 常用 {@link NumberProvider} 变体的紧凑网络编解码器。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, NumberProvider> NUMBER_PROVIDER = StreamCodec.of(
        StreamCodecUtil::numberProviderNetworkEncode,
        StreamCodecUtil::numberProviderNetworkDecode
    );
    public static final StreamCodec<ByteBuf, ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_LOCATION = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        ExtraCodecs.TagOrElementLocation::id,
        ByteBufCodecs.BOOL,
        ExtraCodecs.TagOrElementLocation::tag,
        ExtraCodecs.TagOrElementLocation::new
    );

    /**
     * 将 {@link NumberProvider} 编码为带类型标记的二进制格式。
     *
     * @param buf            输出缓冲区
     * @param numberProvider 待序列化的 provider
     */
    public static void numberProviderNetworkEncode(RegistryFriendlyByteBuf buf, NumberProvider numberProvider) {
        switch (numberProvider) {
            case ConstantValue constantValue -> {
                buf.writeByte(CONSTANT_TYPE);
                buf.writeFloat(constantValue.value());
            }
            case UniformGenerator uniformGenerator -> {
                buf.writeByte(UNIFORM_TYPE);
                StreamCodecUtil.numberProviderNetworkEncode(buf, uniformGenerator.min());
                StreamCodecUtil.numberProviderNetworkEncode(buf, uniformGenerator.max());
            }
            case BinomialDistributionGenerator binomialDistributionGenerator -> {
                buf.writeByte(BINOMIAL_TYPE);
                StreamCodecUtil.numberProviderNetworkEncode(buf, binomialDistributionGenerator.n());
                StreamCodecUtil.numberProviderNetworkEncode(buf, binomialDistributionGenerator.p());
            }
            default -> buf.writeByte(UNKNOWN_TYPE);
        }
    }

    /**
     * 从
     * {@link #numberProviderNetworkEncode(RegistryFriendlyByteBuf, NumberProvider)}.
     * 定义的格式中解码 {@link NumberProvider}。
     *
     * @param buf 输入缓冲区
     * @return 解码后的 provider；未知标记返回 {@code ConstantValue.exactly(1)}
     */
    public static NumberProvider numberProviderNetworkDecode(RegistryFriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case CONSTANT_TYPE -> ConstantValue.exactly(buf.readFloat());
            case UNIFORM_TYPE ->
                new UniformGenerator(StreamCodecUtil.numberProviderNetworkDecode(buf), StreamCodecUtil.numberProviderNetworkDecode(buf));
            case BINOMIAL_TYPE -> new BinomialDistributionGenerator(
                StreamCodecUtil.numberProviderNetworkDecode(buf),
                StreamCodecUtil.numberProviderNetworkDecode(buf)
            );
            default -> ConstantValue.exactly(1);
        };
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityTypePredicate> ENTITY_TYPE_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.holderSet(Registries.ENTITY_TYPE),
        EntityTypePredicate::types,
        EntityTypePredicate::new
    );
    public static final StreamCodec<ByteBuf, DistancePredicate> DISTANCE_PREDICATE = StreamCodec.composite(
        MinMaxBounds.Doubles.STREAM_CODEC,
        DistancePredicate::x,
        MinMaxBounds.Doubles.STREAM_CODEC,
        DistancePredicate::y,
        MinMaxBounds.Doubles.STREAM_CODEC,
        DistancePredicate::z,
        MinMaxBounds.Doubles.STREAM_CODEC,
        DistancePredicate::horizontal,
        MinMaxBounds.Doubles.STREAM_CODEC,
        DistancePredicate::absolute,
        DistancePredicate::new
    );
    public static final StreamCodec<ByteBuf, MovementPredicate> MOVEMENT_PREDICATE = StreamCodec.composite(
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::x,
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::y,
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::z,
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::speed,
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::horizontalSpeed,
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::verticalSpeed,
        MinMaxBounds.Doubles.STREAM_CODEC,
        MovementPredicate::fallDistance,
        MovementPredicate::new
    );
    public static final StreamCodec<ByteBuf, LocationPredicate.PositionPredicate> POSITION_PREDICATE = StreamCodec.composite(
        MinMaxBounds.Doubles.STREAM_CODEC,
        LocationPredicate.PositionPredicate::x,
        MinMaxBounds.Doubles.STREAM_CODEC,
        LocationPredicate.PositionPredicate::y,
        MinMaxBounds.Doubles.STREAM_CODEC,
        LocationPredicate.PositionPredicate::z,
        LocationPredicate.PositionPredicate::new
    );
    public static final StreamCodec<ByteBuf, LightPredicate> LIGHT_PREDICATE = StreamCodec.composite(
        MinMaxBounds.Ints.STREAM_CODEC,
        LightPredicate::composite,
        LightPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidPredicate> FLUID_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.FLUID)),
        FluidPredicate::fluids,
        ByteBufCodecs.optional(StatePropertiesPredicate.STREAM_CODEC),
        FluidPredicate::properties,
        FluidPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LocationPredicate> LOCATION_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.optional(StreamCodecUtil.POSITION_PREDICATE),
        LocationPredicate::position,
        ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BIOME)),
        LocationPredicate::biomes,
        ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.STRUCTURE)),
        LocationPredicate::structures,
        ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)),
        LocationPredicate::dimension,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        LocationPredicate::smokey,
        ByteBufCodecs.optional(StreamCodecUtil.LIGHT_PREDICATE),
        LocationPredicate::light,
        ByteBufCodecs.optional(BlockPredicate.STREAM_CODEC),
        LocationPredicate::block,
        ByteBufCodecs.optional(StreamCodecUtil.FLUID_PREDICATE),
        LocationPredicate::fluid,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        LocationPredicate::canSeeSky,
        LocationPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityPredicate.LocationWrapper> LOCATION_WRAPPER = StreamCodec.composite(
        ByteBufCodecs.optional(StreamCodecUtil.LOCATION_PREDICATE),
        EntityPredicate.LocationWrapper::located,
        ByteBufCodecs.optional(StreamCodecUtil.LOCATION_PREDICATE),
        EntityPredicate.LocationWrapper::steppingOn,
        ByteBufCodecs.optional(StreamCodecUtil.LOCATION_PREDICATE),
        EntityPredicate.LocationWrapper::affectsMovement,
        EntityPredicate.LocationWrapper::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectsPredicate.MobEffectInstancePredicate> MOB_EFFECT_INSTANCE_PREDICATE =
        StreamCodec.composite(
            MinMaxBounds.Ints.STREAM_CODEC,
            MobEffectsPredicate.MobEffectInstancePredicate::amplifier,
            MinMaxBounds.Ints.STREAM_CODEC,
            MobEffectsPredicate.MobEffectInstancePredicate::duration,
            ByteBufCodecs.optional(ByteBufCodecs.BOOL),
            MobEffectsPredicate.MobEffectInstancePredicate::ambient,
            ByteBufCodecs.optional(ByteBufCodecs.BOOL),
            MobEffectsPredicate.MobEffectInstancePredicate::visible,
            MobEffectsPredicate.MobEffectInstancePredicate::new
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectsPredicate> MOB_EFFECTS_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, MobEffect.STREAM_CODEC, StreamCodecUtil.MOB_EFFECT_INSTANCE_PREDICATE),
        MobEffectsPredicate::effectMap,
        MobEffectsPredicate::new
    );
    public static final StreamCodec<ByteBuf, NbtPredicate> NBT_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG,
        NbtPredicate::tag,
        NbtPredicate::new
    );
    public static final StreamCodec<ByteBuf, EntityFlagsPredicate> ENTITY_FLAGS_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isOnGround,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isOnFire,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isCrouching,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isSprinting,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isSwimming,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isFlying,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isBaby,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isInWater,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        EntityFlagsPredicate::isFallFlying,
        EntityFlagsPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPredicate> ITEM_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.ITEM)),
        ItemPredicate::items,
        MinMaxBounds.Ints.STREAM_CODEC,
        ItemPredicate::count,
        DataComponentMatchers.STREAM_CODEC,
        ItemPredicate::components,
        ItemPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityEquipmentPredicate> ENTITY_EQUIPMENT_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::head,
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::chest,
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::legs,
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::feet,
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::body,
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::mainhand,
        ByteBufCodecs.optional(StreamCodecUtil.ITEM_PREDICATE),
        EntityEquipmentPredicate::offhand,
        EntityEquipmentPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EntitySubPredicate> ENTITY_SUB_PREDICATE = StreamCodecUtil.codec2Stream(
        EntitySubPredicate.CODEC
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotsPredicate> SLOTS_PREDICATE = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, StreamCodecUtil.codec2Stream(SlotRanges.CODEC), StreamCodecUtil.ITEM_PREDICATE),
        SlotsPredicate::slots,
        SlotsPredicate::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityPredicate> ENTITY_PREDICATE = StreamCodec.recursive(parent -> StreamCodecUtil.composite(
        ByteBufCodecs.optional(StreamCodecUtil.ENTITY_TYPE_PREDICATE),
        EntityPredicate::entityType,
        ByteBufCodecs.optional(StreamCodecUtil.DISTANCE_PREDICATE),
        EntityPredicate::distanceToPlayer,
        ByteBufCodecs.optional(StreamCodecUtil.MOVEMENT_PREDICATE),
        EntityPredicate::movement,
        StreamCodecUtil.LOCATION_WRAPPER,
        EntityPredicate::location,
        ByteBufCodecs.optional(StreamCodecUtil.MOB_EFFECTS_PREDICATE),
        EntityPredicate::effects,
        ByteBufCodecs.optional(StreamCodecUtil.NBT_PREDICATE),
        EntityPredicate::nbt,
        ByteBufCodecs.optional(StreamCodecUtil.ENTITY_FLAGS_PREDICATE),
        EntityPredicate::flags,
        ByteBufCodecs.optional(StreamCodecUtil.ENTITY_EQUIPMENT_PREDICATE),
        EntityPredicate::equipment,
        ByteBufCodecs.optional(StreamCodecUtil.ENTITY_SUB_PREDICATE),
        EntityPredicate::subPredicate,
        ByteBufCodecs.optional(ByteBufCodecs.VAR_INT),
        EntityPredicate::periodicTick,
        ByteBufCodecs.optional(parent),
        EntityPredicate::vehicle,
        ByteBufCodecs.optional(parent),
        EntityPredicate::passenger,
        ByteBufCodecs.optional(parent),
        EntityPredicate::targetedEntity,
        ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
        EntityPredicate::team,
        ByteBufCodecs.optional(StreamCodecUtil.SLOTS_PREDICATE),
        EntityPredicate::slots,
        DataComponentMatchers.STREAM_CODEC,
        EntityPredicate::components,
        EntityPredicate::new
    ));
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageSourcePredicate> DAMAGE_SOURCE_PREDICATE = StreamCodec.composite(
        StreamCodecUtil.tagPredicate(Registries.DAMAGE_TYPE).apply(ByteBufCodecs.list()),
        DamageSourcePredicate::tags,
        ByteBufCodecs.optional(StreamCodecUtil.ENTITY_PREDICATE),
        DamageSourcePredicate::directEntity,
        ByteBufCodecs.optional(StreamCodecUtil.ENTITY_PREDICATE),
        DamageSourcePredicate::sourceEntity,
        ByteBufCodecs.optional(ByteBufCodecs.BOOL),
        DamageSourcePredicate::isDirect,
        DamageSourcePredicate::new
    );

    public static <T> StreamCodec<ByteBuf, TagPredicate<T>> tagPredicate(ResourceKey<? extends Registry<T>> registryKey) {
        return StreamCodec.composite(
            TagKey.streamCodec(registryKey),
            TagPredicate::tag,
            ByteBufCodecs.BOOL,
            TagPredicate::expected,
            TagPredicate::new
        );
    }

    /**
     * 将普通 {@link Codec} 包装为基于 NBT 中间层的 {@link StreamCodec}。
     *
     * @param codec 待桥接的 codec
     * @param <T>   负载类型
     * @return 以 NBT 作为中间表示的 stream codec
     */
    public static <T> StreamCodec<? super FriendlyByteBuf, T> nbtWrapped(Codec<T> codec) {

        return new StreamCodec<>() {
            @Override
            public T decode(FriendlyByteBuf buffer) {
                return codec.decode(NbtOps.INSTANCE, buffer.readNbt()).getOrThrow().getFirst();
            }

            @Override
            public void encode(FriendlyByteBuf buffer, T value) {
                buffer.writeNbt(codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
            }
        };
    }

    /**
     * 创建基于 ordinal 序号的枚举 stream codec。
     *
     * @param clazz 枚举类
     * @param <B>   缓冲区类型
     * @param <T>   枚举类型
     * @return 使用 var-int ordinal 的枚举 stream codec
     */
    public static <B extends ByteBuf, T extends Enum<T>> StreamCodec<B, T> enumStreamCodec(Class<T> clazz) {
        return ByteBufCodecs.VAR_INT.<B>cast().map(index -> clazz.getEnumConstants()[index], Enum::ordinal);
    }

    /**
     * 由两个组件 codec 组合出 {@link Pair} codec。
     */
    public static <B, F, S> StreamCodec<B, Pair<F, S>> createPairStreamCodec(
        StreamCodec<? super B, F> first,
        StreamCodec<? super B, S> second
    ) {
        return StreamCodec.composite(first, Pair::getFirst, second, Pair::getSecond, Pair::new);
    }

    /**
     * 将带注册表上下文的 {@link Codec} 通过 NBT 中转转换为 {@link StreamCodec}。
     *
     * @param codec 源 codec
     * @param <T>   负载类型
     * @return 适用于注册表感知网络上下文的 stream codec
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> codec2Stream(Codec<T> codec) {
        return StreamCodec.of(
            (buffer, value) -> {
                RegistryOps<Tag> context = buffer.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                Tag tag = codec.encodeStart(context, value).getOrThrow();
                buffer.writeNbt(tag);
            }, buffer -> {
                RegistryOps<Tag> context = buffer.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                return codec.decode(context, buffer.readNbt()).getOrThrow().getFirst();
            }
        );
    }

    /**
     * 使用 7 个组件 codec 组合生成一个聚合 stream codec。
     *
     * <p>组件顺序是严格的位置语义：构造参数顺序、decode 顺序、encode 顺序和
     * factory 参数顺序必须保持一致。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
            }
        };
    }

    /**
     * 8 参数版本，语义同 7 参数 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
            }
        };
    }

    /**
     * 9 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
            }
        };
    }

    /**
     * 10 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
            }
        };
    }

    /**
     * 11 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
            }
        };
    }

    /**
     * 12 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final StreamCodec<? super B, T12> codec12,
        final Function<C, T12> getter12,
        final Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
                codec12.encode(buffer, getter12.apply(value));
            }
        };
    }

    /**
     * 13 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final StreamCodec<? super B, T12> codec12,
        final Function<C, T12> getter12,
        final StreamCodec<? super B, T13> codec13,
        final Function<C, T13> getter13,
        final Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
                codec12.encode(buffer, getter12.apply(value));
                codec13.encode(buffer, getter13.apply(value));
            }
        };
    }

    /**
     * 14 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final StreamCodec<? super B, T12> codec12,
        final Function<C, T12> getter12,
        final StreamCodec<? super B, T13> codec13,
        final Function<C, T13> getter13,
        final StreamCodec<? super B, T14> codec14,
        final Function<C, T14> getter14,
        final Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                T14 t14 = codec14.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
                codec12.encode(buffer, getter12.apply(value));
                codec13.encode(buffer, getter13.apply(value));
                codec14.encode(buffer, getter14.apply(value));
            }
        };
    }

    /**
     * 15 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final StreamCodec<? super B, T12> codec12,
        final Function<C, T12> getter12,
        final StreamCodec<? super B, T13> codec13,
        final Function<C, T13> getter13,
        final StreamCodec<? super B, T14> codec14,
        final Function<C, T14> getter14,
        final StreamCodec<? super B, T15> codec15,
        final Function<C, T15> getter15,
        final Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                T14 t14 = codec14.decode(buffer);
                T15 t15 = codec15.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
                codec12.encode(buffer, getter12.apply(value));
                codec13.encode(buffer, getter13.apply(value));
                codec14.encode(buffer, getter14.apply(value));
                codec15.encode(buffer, getter15.apply(value));
            }
        };
    }

    /**
     * 16 参数版本的 {@code composite(...)}。
     */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final StreamCodec<? super B, T12> codec12,
        final Function<C, T12> getter12,
        final StreamCodec<? super B, T13> codec13,
        final Function<C, T13> getter13,
        final StreamCodec<? super B, T14> codec14,
        final Function<C, T14> getter14,
        final StreamCodec<? super B, T15> codec15,
        final Function<C, T15> getter15,
        final StreamCodec<? super B, T16> codec16,
        final Function<C, T16> getter16,
        final Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, C> factory
    ) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                T14 t14 = codec14.decode(buffer);
                T15 t15 = codec15.decode(buffer);
                T16 t16 = codec16.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
                codec11.encode(buffer, getter11.apply(value));
                codec12.encode(buffer, getter12.apply(value));
                codec13.encode(buffer, getter13.apply(value));
                codec14.encode(buffer, getter14.apply(value));
                codec15.encode(buffer, getter15.apply(value));
                codec16.encode(buffer, getter16.apply(value));
            }
        };
    }
}
