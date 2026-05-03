package dev.anvilcraft.lib.v2.util.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 表示一个带有概率的方块状态
 * <p>
 * 该类用于定义在配方中可能出现的方块结果，包含方块状态和出现概率
 * </p>
 *
 * @param state  方块状态
 * @param nbt    方块的 NBT 数据
 * @param chance 出现概率
 */
public record ChanceBlockState(BlockState state, CompoundTag nbt, NumberProvider chance) {
    /**
     * 构造一个带有概率的方块状态
     *
     * @param state  方块状态
     * @param nbt    方块的 NBT 数据
     * @param chance 出现概率
     */
    public ChanceBlockState {
    }

    /**
     * 构造一个带有固定概率的方块状态
     *
     * @param state  方块状态
     * @param chance 出现概率（固定值）
     */
    public ChanceBlockState(BlockState state, NumberProvider chance) {
        this(state, new CompoundTag(), chance);
    }

    /**
     * 构造一个带有固定概率的方块状态
     *
     * @param state  方块状态
     * @param chance 出现概率（固定值）
     */
    public ChanceBlockState(BlockState state, float chance) {
        this(state, ConstantValue.exactly(chance));
    }

    public static ChanceBlockState of(Supplier<? extends Block> block, CompoundTag nbt) {
        return new ChanceBlockState(block.get().defaultBlockState(), nbt, ConstantValue.exactly(1.0f));
    }

    public static ChanceBlockState of(Supplier<? extends Block> block) {
        return ChanceBlockState.of(block, new CompoundTag());
    }

    /**
     * ChanceBlockState的编解码器
     */
    public static final MapCodec<ChanceBlockState> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            CodecUtil.BLOCK_STATE_MAP_CODEC
                .forGetter(ChanceBlockState::state),
            CompoundTag.CODEC
                .optionalFieldOf("nbt", new CompoundTag())
                .forGetter(ChanceBlockState::nbt),
            CodecUtil.NUMBER_PROVIDER
                .optionalFieldOf("chance", ConstantValue.exactly(1.0f))
                .forGetter(ChanceBlockState::chance)
        ).apply(instance, ChanceBlockState::new));

    /**
     * ChanceBlockState的网络流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceBlockState> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.BLOCK_STATE,
        ChanceBlockState::state,
        ByteBufCodecs.COMPOUND_TAG,
        ChanceBlockState::nbt,
        StreamCodecUtil.NUMBER_PROVIDER,
        ChanceBlockState::chance,
        ChanceBlockState::new
    );

    public @Nullable Map.Entry<BlockState, CompoundTag> getResult(ServerLevel level) {
        LootContext context = new LootContext.Builder(
            new LootParams(
                level,
                new ContextMap.Builder().create(new ContextKeySet.Builder().build()),
                Map.of(),
                0
            )
        ).create(Optional.empty());
        if (level.getRandom().nextFloat() > this.chance.getFloat(context)) return null;
        return Map.entry(this.state, this.nbt);
    }
}