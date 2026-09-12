package dev.anvilcraft.lib.v2.util.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A list of weighted ChanceBlockState entries. Selection is done by weights (NumberProvider),
 * and the selected entry's own chance is honored by delegating to ChanceBlockState#getResult.
 */
public record WeightedChanceBlockStates(List<Entry> states) {
    public static final MapCodec<WeightedChanceBlockStates> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Entry.CODEC.codec()
            .listOf()
            .fieldOf("states")
            .forGetter(WeightedChanceBlockStates::states)
    ).apply(inst, WeightedChanceBlockStates::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeightedChanceBlockStates> STREAM_CODEC = StreamCodec.composite(
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        WeightedChanceBlockStates::states,
        WeightedChanceBlockStates::new
    );

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Pick one entry according to their weights and return the selected entry's result
     * (which may still be null if the ChanceBlockState's own chance check fails).
     */
    public @Nullable Map.Entry<BlockState, CompoundTag> getResult(ServerLevel level) {
        LootContext context = new LootContext.Builder(new LootParams(level, ContextMap.EMPTY, Map.of(), 0)).create(Optional.empty());

        float total = 0f;
        for (Entry e : this.states) {
            float w = e.weight.getFloat(context);
            if (w > 0f) total += w;
        }
        if (total <= 0f) return null;

        float r = level.getRandom().nextFloat() * total;
        float accum = 0f;
        for (Entry e : this.states) {
            float w = e.weight.getFloat(context);
            if (w <= 0f) continue;
            accum += w;
            if (r <= accum) {
                return e.state.getResult(level);
            }
        }
        return null;
    }

    public record Entry(ChanceBlockState state, NumberProvider weight) {
        public static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ChanceBlockState.CODEC.codec()
                .fieldOf("state")
                .forGetter(Entry::state),
            CodecUtil.NUMBER_PROVIDER
                .optionalFieldOf("weight", ConstantValue.exactly(1.0f))
                .forGetter(Entry::weight)
        ).apply(inst, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            ChanceBlockState.STREAM_CODEC,
            Entry::state,
            StreamCodecUtil.NUMBER_PROVIDER,
            Entry::weight,
            Entry::new
        );
    }

    public static class Builder {
        private final ImmutableList.Builder<Entry> states = ImmutableList.builder();

        public Builder add(NumberProvider weight, ChanceBlockState state) {
            this.states.add(new Entry(state, weight));
            return this;
        }

        public Builder add(float weight, ChanceBlockState state) {
            return this.add(ConstantValue.exactly(weight), state);
        }

        public Builder add(ChanceBlockState state) {
            return this.add(1.0F, state);
        }

        public Builder add(NumberProvider weight, BlockState state, CompoundTag nbt, NumberProvider chance) {
            return this.add(weight, new ChanceBlockState(state, nbt, chance));
        }

        public Builder add(float weight, BlockState state, CompoundTag nbt, NumberProvider chance) {
            return this.add(ConstantValue.exactly(weight), state, nbt, chance);
        }

        public Builder add(BlockState state, CompoundTag nbt, NumberProvider chance) {
            return this.add(1.0F, state, nbt, chance);
        }

        public Builder add(NumberProvider weight, BlockState state, CompoundTag nbt, float chance) {
            return this.add(weight, state, nbt, ConstantValue.exactly(chance));
        }

        public Builder add(float weight, BlockState state, CompoundTag nbt, float chance) {
            return this.add(ConstantValue.exactly(weight), state, nbt, chance);
        }

        public Builder add(BlockState state, CompoundTag nbt, float chance) {
            return this.add(1.0F, state, nbt, chance);
        }

        public Builder add(NumberProvider weight, BlockState state, float chance) {
            return this.add(weight, state, new CompoundTag(), chance);
        }

        public Builder add(float weight, BlockState state, float chance) {
            return this.add(ConstantValue.exactly(weight), state, chance);
        }

        public Builder add(BlockState state, float chance) {
            return this.add(1.0F, state, chance);
        }

        public Builder add(NumberProvider weight, BlockState state) {
            return this.add(weight, state, 1.0F);
        }

        public Builder add(float weight, BlockState state) {
            return this.add(ConstantValue.exactly(weight), state);
        }

        public Builder add(BlockState state) {
            return this.add(1.0F, state);
        }

        public Builder add(NumberProvider weight, Block block, CompoundTag nbt, NumberProvider chance) {
            return this.add(weight, block.defaultBlockState(), nbt, chance);
        }

        public Builder add(float weight, Block block, CompoundTag nbt, NumberProvider chance) {
            return this.add(ConstantValue.exactly(weight), block, nbt, chance);
        }

        public Builder add(Block block, CompoundTag nbt, NumberProvider chance) {
            return this.add(1.0F, block, nbt, chance);
        }

        public Builder add(NumberProvider weight, Block block, CompoundTag nbt, float chance) {
            return this.add(weight, block, nbt, ConstantValue.exactly(chance));
        }

        public Builder add(float weight, Block block, CompoundTag nbt, float chance) {
            return this.add(ConstantValue.exactly(weight), block, nbt, chance);
        }

        public Builder add(Block block, CompoundTag nbt, float chance) {
            return this.add(1.0F, block, nbt, chance);
        }

        public Builder add(NumberProvider weight, Block block, float chance) {
            return this.add(weight, block, new CompoundTag(), chance);
        }

        public Builder add(float weight, Block block, float chance) {
            return this.add(ConstantValue.exactly(weight), block, chance);
        }

        public Builder add(Block block, float chance) {
            return this.add(1.0F, block, chance);
        }

        public Builder add(NumberProvider weight, Block block) {
            return this.add(weight, block, 1.0F);
        }

        public Builder add(float weight, Block block) {
            return this.add(ConstantValue.exactly(weight), block);
        }

        public Builder add(Block block) {
            return this.add(1.0F, block);
        }

        public Builder add(NumberProvider weight, Supplier<? extends Block> block, CompoundTag nbt, NumberProvider chance) {
            return this.add(weight, block.get(), nbt, chance);
        }

        public Builder add(float weight, Supplier<? extends Block> block, CompoundTag nbt, NumberProvider chance) {
            return this.add(ConstantValue.exactly(weight), block, nbt, chance);
        }

        public Builder add(Supplier<? extends Block> block, CompoundTag nbt, NumberProvider chance) {
            return this.add(1.0F, block, nbt, chance);
        }

        public Builder add(NumberProvider weight, Supplier<? extends Block> block, CompoundTag nbt, float chance) {
            return this.add(weight, block, nbt, ConstantValue.exactly(chance));
        }

        public Builder add(float weight, Supplier<? extends Block> block, CompoundTag nbt, float chance) {
            return this.add(ConstantValue.exactly(weight), block, nbt, chance);
        }

        public Builder add(Supplier<? extends Block> block, CompoundTag nbt, float chance) {
            return this.add(1.0F, block, nbt, chance);
        }

        public Builder add(NumberProvider weight, Supplier<? extends Block> block, float chance) {
            return this.add(weight, block, new CompoundTag(), chance);
        }

        public Builder add(float weight, Supplier<? extends Block> block, float chance) {
            return this.add(ConstantValue.exactly(weight), block, chance);
        }

        public Builder add(Supplier<? extends Block> block, float chance) {
            return this.add(1.0F, block, chance);
        }

        public Builder add(NumberProvider weight, Supplier<? extends Block> block) {
            return this.add(weight, block, 1.0F);
        }

        public Builder add(float weight, Supplier<? extends Block> block) {
            return this.add(ConstantValue.exactly(weight), block);
        }

        public Builder add(Supplier<? extends Block> block) {
            return this.add(1.0F, block);
        }

        public boolean isEmpty() {
            return this.states.build().isEmpty();
        }

        public WeightedChanceBlockStates build() {
            return new WeightedChanceBlockStates(this.states.build());
        }
    }
}
