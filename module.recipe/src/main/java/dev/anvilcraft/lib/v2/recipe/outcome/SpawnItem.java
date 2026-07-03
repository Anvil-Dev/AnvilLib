package dev.anvilcraft.lib.v2.recipe.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheOutput;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeOutcomeTypes;
import dev.anvilcraft.lib.v2.recipe.outcome.function.ApplyTagToComponent;
import dev.anvilcraft.lib.v2.recipe.outcome.function.IOutcomeFunction;
import dev.anvilcraft.lib.v2.recipe.util.IRecipeResultOffsetBlock;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * 生成物品配方结果类，用于定义在配方执行时生成物品的结果
 * 该类实现了 IRecipeOutcome 接口，可以根据偏移量和数量生成指定的物品
 *
 * @param item      物品堆
 * @param offset    偏移量
 * @param count     数量
 * @param functions 函数列表
 */
public record SpawnItem(ItemStackTemplate item, Vec3 offset, NumberProvider count, List<IOutcomeFunction<?>> functions)
    implements IRecipeOutcome<SpawnItem> {
    /**
     * 构造一个新的生成物品配方结果
     *
     * @param item   物品堆
     * @param offset 偏移量
     * @param count  数量
     */
    public SpawnItem {
    }

    /**
     * 构造一个新的生成物品配方结果
     *
     * @param item   物品持有者
     * @param patch  数据组件补丁
     * @param offset 偏移量
     * @param count  数量
     */
    public SpawnItem(Holder<Item> item, DataComponentPatch patch, Vec3 offset, NumberProvider count, List<IOutcomeFunction<?>> functions) {
        this(new ItemStackTemplate(item, 1, patch), offset, count, functions);
    }

    /**
     * 创建一个新的生成物品配方结果构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 将此ChanceItemStack转换为SpawnItem结果
     *
     * @param offset 偏移量
     * @return SpawnItem结果
     */
    public static SpawnItem fromChance(ChanceItemStack stack, Vec3 offset) {
        return SpawnItem.builder().item(stack.stack()).count(stack.count()).offset(offset).build();
    }

    /**
     * 获取配方结果类型
     *
     * @return 配方结果类型
     */
    @Override
    public Type getType() {
        return LibRecipeOutcomeTypes.SPAWN_ITEM.get();
    }

    /**
     * 接受配方上下文并处理生成物品的结果
     *
     * @param context 配方上下文
     */
    @Override
    @SuppressWarnings("unchecked")
    public void accept(InWorldRecipeContext context) {
        ItemCache cache = context.computeIfAbsent(ItemCache.ITEM_CACHE);
        int count = context.getInt(this.count, 0, 99);
        if (count == 0) return;
        ItemStack stack = this.item.withCount(count).create();
        BlockCache blockCache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        Vec3 offset = context.getPos().add(this.offset);
        BlockPos blockPos = BlockPos.containing(offset);
        BlockState state = blockCache.getBlockState(blockPos);
        if (state.getBlock() instanceof IRecipeResultOffsetBlock block) {
            Vec3 offset1 = block.getOffset(context.getLevel(), blockPos, state);
            offset = offset.add(offset1);
        }
        for (IOutcomeFunction<?> function : this.functions) {
            IOutcomeFunction<ItemStack> function1 = (IOutcomeFunction<ItemStack>) function;
            stack = function1.apply(context, stack);
        }
        ICacheOutput output = cache.getOutput(stack, offset);
        output.grow(stack, true);
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
    }

    /**
     * 生成物品配方结果类型类
     */
    public static class Type implements IRecipeOutcome.Type<SpawnItem> {
        /**
         * Map编解码器
         */
        private static final MapCodec<SpawnItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Item.CODEC
                    .fieldOf("item")
                    .forGetter(spawnItem -> spawnItem.item().typeHolder()),
                DataComponentPatch.CODEC
                    .optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(spawnItem -> spawnItem.item().components()),
                Vec3.CODEC
                    .fieldOf("offset")
                    .forGetter(SpawnItem::offset),
                CodecUtil.NUMBER_PROVIDER
                    .optionalFieldOf("count", ConstantValue.exactly(1.0f))
                    .forGetter(SpawnItem::count),
                IOutcomeFunction.CODEC
                    .listOf()
                    .optionalFieldOf("functions", List.of())
                    .forGetter(SpawnItem::functions)
            ).apply(instance, SpawnItem::new)
        );

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, SpawnItem> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            SpawnItem::item,
            StreamCodecUtil.VEC3,
            SpawnItem::offset,
            StreamCodecUtil.NUMBER_PROVIDER,
            SpawnItem::count,
            StreamCodecUtil.codec2Stream(IOutcomeFunction.CODEC).apply(ByteBufCodecs.list()),
            SpawnItem::functions,
            SpawnItem::new
        );

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public MapCodec<SpawnItem> codec() {
            return Type.CODEC;
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SpawnItem> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

    /**
     * 生成物品配方结果构建器类
     */
    public static class Builder {
        /**
         * 偏移量
         */
        private Vec3 offset = Vec3.ZERO;

        /**
         * 数量
         */
        private NumberProvider count = ConstantValue.exactly(1.0f);

        /**
         * 物品堆
         */
        private @Nullable ItemStackTemplate item = null;

        /**
         * 函数列表
         */
        private final List<IOutcomeFunction<?>> functions = new ArrayList<>();

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
         * 设置数量
         *
         * @param count 数量
         * @return 构建器实例
         */
        public Builder count(NumberProvider count) {
            this.count = count;
            return this;
        }

        /**
         * 设置数量
         *
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder count(float chance) {
            return this.count(ConstantValue.exactly(chance));
        }

        /**
         * 设置物品堆
         *
         * @param item 物品堆
         * @return 构建器实例
         */
        public Builder item(ItemStackTemplate item) {
            this.item = item;
            return this;
        }

        /**
         * 设置物品
         *
         * @param item 物品
         * @return 构建器实例
         */
        public Builder item(Item item) {
            return this.item(new ItemStackTemplate(item));
        }

        /**
         * 设置物品
         *
         * @param item 物品
         * @return 构建器实例
         */
        public Builder item(ItemLike item) {
            return this.item(item.asItem());
        }

        public Builder function(IOutcomeFunction<?>... function) {
            this.functions.addAll(Arrays.asList(function));
            return this;
        }

        public Builder applyComponent(DataComponentType<?> component, Identifier path) {
            this.functions.add(new ApplyTagToComponent<>(component, path));
            return this;
        }

        /**
         * 构建生成物品配方结果
         *
         * @return 生成物品配方结果
         */
        public SpawnItem build() {
            return new SpawnItem(Objects.requireNonNull(this.item), this.offset, this.count, this.functions);
        }
    }
}
