package dev.anvilcraft.lib.v2.util.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.MinMaxBounds;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 物品谓词
 * <p>
 * 用于定义物品匹配规则，包括物品类型、数量范围、组件和子谓词
 * </p>
 *
 * @param items      物品集合
 * @param count      数量范围
 * @param components 数据组件谓词
 */
public record ItemPredicate(
    Optional<HolderSet<Item>> items, MinMaxBounds.Ints count, DataComponentMatchers components
) implements IItemStackPredicate {
    /**
     * ItemPredicate编解码器
     */
    public static final Codec<ItemPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemPredicate::items),
        MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(ItemPredicate::count),
        DataComponentMatchers.CODEC.codec().optionalFieldOf("components", DataComponentMatchers.ANY).forGetter(ItemPredicate::components)
    ).apply(instance, ItemPredicate::new));

    /**
     * ItemPredicate流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPredicate> STREAM_CODEC = StreamCodecUtil.codec2Stream(ItemPredicate.CODEC);

    @Override
    public boolean test(ItemStack itemStack) {
        return this.testIgnoreCount(itemStack) && this.testCount(itemStack.count());
    }

    @Override
    public boolean testCount(int count) {
        return this.count.matches(count);
    }

    /**
     * 构建器类，用于构建ItemPredicate实例
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<HolderSet<Item>> items = Optional.empty();
        private MinMaxBounds.Ints count;
        private DataComponentMatchers components;

        /**
         * 构造一个构建器
         */
        private Builder() {
            this.count = MinMaxBounds.Ints.ANY;
            this.components = DataComponentMatchers.ANY;
        }

        /**
         * 创建一个物品构建器
         *
         * @return 构建器实例
         */
        public static Builder item() {
            return new Builder();
        }

        /**
         * 设置物品
         *
         * @param items 物品数组
         * @return 构建器实例
         */
        public Builder of(ItemLike... items) {
            //noinspection deprecation
            this.items = Optional.of(HolderSet.direct((item) -> item.asItem().builtInRegistryHolder(), items));
            return this;
        }

        /**
         * 设置物品标签
         *
         * @param tag 物品标签
         * @return 构建器实例
         */
        public Builder of(TagKey<Item> tag) {
            List<Holder<Item>> holders = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holders::add);
            //noinspection deprecation
            this.items = Optional.of(HolderSet.direct((item) -> item.value().builtInRegistryHolder(), holders));
            return this;
        }

        /**
         * 设置数量范围
         *
         * @param count 数量范围
         * @return 构建器实例
         */
        public Builder withCount(MinMaxBounds.Ints count) {
            this.count = count;
            return this;
        }

        /**
         * 设置数据组件谓词
         *
         * @param components 数据组件谓词
         * @return 构建器实例
         */
        public Builder hasComponents(DataComponentMatchers components) {
            this.components = components;
            return this;
        }

        /**
         * 构建ItemPredicate实例
         *
         * @return ItemPredicate实例
         */
        public ItemPredicate build() {
            return new ItemPredicate(this.items, this.count, this.components);
        }
    }
}