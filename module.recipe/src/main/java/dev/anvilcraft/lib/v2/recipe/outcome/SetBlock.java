package dev.anvilcraft.lib.v2.recipe.outcome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeOutcomeTypes;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设置方块配方结果类，用于定义在配方执行时设置方块的结果
 * 该类实现了 IRecipeOutcome 接口，可以根据偏移量和概率设置指定位置的方块
 *
 * @param state  方块状态
 * @param nbt    NBT 数据
 * @param offset 偏移量
 * @param chance 概率
 */
@Slf4j
public record SetBlock(
    BlockState state, CompoundTag nbt, Vec3 offset, ContextFloatProvider chance,
    boolean copyProperties, Map<String, BlockPropertyMapping> propertyMappings
) implements IRecipeOutcome<SetBlock> {
    /**
     * 构造一个新的设置方块配方结果
     *
     * @param state  方块状态
     * @param offset 偏移量
     * @param chance 概率
     */
    public SetBlock {
        nbt = nbt == null ? new CompoundTag() : nbt.copy();
        propertyMappings = Map.copyOf(propertyMappings);
        propertyMappings.forEach((target, mapping) -> {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(target);
            if (property == null) throw new IllegalArgumentException("Unknown target property: " + target);
            for (String value : mapping.values().values()) {
                if (property.getValue(value).isEmpty()) {
                    throw new IllegalArgumentException("Invalid target property value: " + target + "=" + value);
                }
            }
        });
    }

    /** 保留旧构造器的二进制签名及不继承属性的行为。 */
    public SetBlock(BlockState state, CompoundTag nbt, Vec3 offset, ContextFloatProvider chance) {
        this(state, nbt, offset, chance, false, Map.of());
    }

    /** 先继承兼容的同名属性，再应用显式映射。 */
    public BlockState transform(BlockState source) {
        BlockState result = this.state;
        if (this.copyProperties) {
            for (Property<?> property : source.getProperties()) {
                result = BlockPropertyMapping.set(result, property.getName(), BlockPropertyMapping.name(source, property));
            }
        }
        for (Map.Entry<String, BlockPropertyMapping> entry : this.propertyMappings.entrySet()) {
            result = entry.getValue().apply(source, result, entry.getKey());
        }
        return result;
    }

    /**
     * 创建一个新的设置方块配方结果构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    public static SetBlock fromState(ChanceBlockState state, Vec3 offset) {
        return SetBlock.builder().block(state.state()).offset(offset).nbt(state.nbt()).chance(state.chance()).build();
    }

    /**
     * 获取配方结果类型
     *
     * @return 配方结果类型
     */
    @Override
    public Type getType() {
        return LibRecipeOutcomeTypes.SET_BLOCK.get();
    }

    /**
     * 接受配方上下文并处理设置方块的结果
     *
     * @param context 配方上下文
     */
    @Override
    public void accept(InWorldRecipeContext context) {
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        BlockPos blockPos = BlockPos.containing(context.getPos().add(this.offset));
        List<BlockPos> positions;
        if (this.copyProperties || !this.propertyMappings.isEmpty()) {
            positions = cache.transformBlock(blockPos, this::transform);
        } else {
            cache.setBlock(blockPos, this.state);
            positions = List.of(blockPos);
        }
        for (BlockPos pos : positions) {
            BlockState result = cache.getBlockState(pos);
            if (!(result.getBlock() instanceof EntityBlock entityBlock)) continue;
            BlockEntity entity = entityBlock.newBlockEntity(pos, result);
            if (entity != null) {
                try (
                    ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), log)
                ) {
                    ValueInput input = TagValueInput.create(reporter, context.getLevel().registryAccess(), this.nbt);
                    entity.loadWithComponents(input);
                    cache.setBlockEntity(pos, entity);
                }
            }
        }
        context.putAcceptor(BlockCache.BLOCK_CACHE.location(), BlockCache.DEFAULT_ACCEPTOR);
    }

    /**
     * 设置方块配方结果类型类
     */
    public static class Type implements IRecipeOutcome.Type<SetBlock> {
        /**
         * Map编解码器
         */
        private static final MapCodec<SetBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                CodecUtil.BLOCK_STATE_MAP_CODEC
                    .forGetter(SetBlock::state),
                CompoundTag.CODEC
                    .optionalFieldOf("nbt", new CompoundTag())
                    .forGetter(SetBlock::nbt),
                Vec3.CODEC
                    .fieldOf("offset")
                    .forGetter(SetBlock::offset),
                CodecUtil.FLOAT_NUMBER_PROVIDER
                    .optionalFieldOf("chance", new ConstantValue(1.0f))
                    .forGetter(SetBlock::chance),
                Codec.BOOL.optionalFieldOf("copy_properties", false).forGetter(SetBlock::copyProperties),
                Codec.unboundedMap(Codec.STRING, BlockPropertyMapping.CODEC)
                    .optionalFieldOf("property_mappings", Map.of()).forGetter(SetBlock::propertyMappings)
            ).apply(instance, SetBlock::new));

        /**
         * 流编解码器
         */
        private static final StreamCodec<RegistryFriendlyByteBuf, SetBlock> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.BLOCK_STATE,
            SetBlock::state,
            ByteBufCodecs.COMPOUND_TAG,
            SetBlock::nbt,
            StreamCodecUtil.VEC3,
            SetBlock::offset,
            StreamCodecUtil.FLOAT_NUMBER_PROVIDER,
            SetBlock::chance,
            ByteBufCodecs.BOOL,
            SetBlock::copyProperties,
            StreamCodecUtil.codec2Stream(Codec.unboundedMap(Codec.STRING, BlockPropertyMapping.CODEC)),
            SetBlock::propertyMappings,
            SetBlock::new
        );

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public MapCodec<SetBlock> codec() {
            return Type.CODEC;
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SetBlock> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    /**
     * 设置方块配方结果构建器类
     */
    public static class Builder {
        /**
         * 方块状态
         */
        private BlockState state = Blocks.AIR.defaultBlockState();

        /**
         * NBT 数据
         */
        private CompoundTag nbt;

        private boolean copyProperties;
        private final Map<String, BlockPropertyMapping> propertyMappings = new LinkedHashMap<>();

        public Builder copyProperties() {
            return this.copyProperties(true);
        }

        public Builder copyProperties(boolean copy) {
            this.copyProperties = copy;
            return this;
        }

        public Builder propertyMappings(Map<String, BlockPropertyMapping> mappings) {
            this.propertyMappings.putAll(mappings);
            return this;
        }

        public Builder mapProperty(String source, String target, Map<String, String> values) {
            this.propertyMappings.put(target, new BlockPropertyMapping(source, values));
            return this;
        }

        public Builder mapProperty(String source, String target) {
            return this.mapProperty(source, target, Map.of());
        }

        public <S extends Comparable<S>, T extends Comparable<T>> Builder mapProperty(
            Property<S> source, Property<T> target, Map<S, T> values
        ) {
            Map<String, String> names = new LinkedHashMap<>();
            values.forEach((from, to) -> names.put(source.getName(from), target.getName(to)));
            return this.mapProperty(source.getName(), target.getName(), names);
        }

        public Builder mapProperty(Property<?> source, Property<?> target) {
            return this.mapProperty(source.getName(), target.getName());
        }

        /**
         * 偏移量
         */
        private Vec3 offset = Vec3.ZERO;

        /**
         * 概率
         */
        private ContextFloatProvider chance = new ConstantValue(1.0f);

        /**
         * 设置偏移量
         *
         * @param offset 偏移量
         * @return 构建器实例
         */
        public Builder offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }

        /**
         * 设置偏移量
         *
         * @param x X轴偏移量
         * @param y Y轴偏移量
         * @param z Z轴偏移量
         * @return 构建器实例
         */
        public Builder offset(double x, double y, double z) {
            this.offset = new Vec3(x, y, z);
            return this;
        }

        /**
         * 设置向下偏移量
         *
         * @param below 向下偏移量
         * @return 构建器实例
         */
        public Builder below(double below) {
            return this.offset(Vec3.ZERO.subtract(0, below, 0));
        }

        /**
         * 设置向下偏移1格
         *
         * @return 构建器实例
         */
        public Builder below() {
            return this.below(1);
        }

        /**
         * 设置向上偏移量
         *
         * @param above 向上偏移量
         * @return 构建器实例
         */
        public Builder above(double above) {
            return this.offset(Vec3.ZERO.add(0, above, 0));
        }

        /**
         * 设置向上偏移1格
         *
         * @return 构建器实例
         */
        public Builder above() {
            return this.above(1);
        }

        /**
         * 设置概率
         *
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder chance(ContextFloatProvider chance) {
            this.chance = chance;
            return this;
        }

        /**
         * 设置概率
         *
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder chance(float chance) {
            return this.chance(new ConstantValue(chance));
        }

        /**
         * 设置方块状态
         *
         * @param state 方块状态
         * @return 构建器实例
         */
        public Builder block(BlockState state) {
            this.state = state;
            return this;
        }

        /**
         * 设置方块
         *
         * @param block 方块
         * @return 构建器实例
         */
        public Builder block(Block block) {
            this.state = block.defaultBlockState();
            return this;
        }

        /**
         * 设置NBT数据
         *
         * @param nbt NBT数据
         * @return 构建器实例
         */
        public Builder nbt(CompoundTag nbt) {
            this.nbt = nbt;
            return this;
        }

        /**
         * 构建设置方块配方结果
         *
         * @return 设置方块配方结果
         */
        public SetBlock build() {
            return new SetBlock(state, nbt, offset, chance, copyProperties, propertyMappings);
        }
    }
}
