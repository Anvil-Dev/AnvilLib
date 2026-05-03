package dev.anvilcraft.lib.v2.util.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 物品原料谓词
 * <p>
 * 用于定义配方中物品原料的匹配规则，包括物品类型、数量、组件和子谓词
 * </p>
 *
 * @param items      物品集合
 * @param count      数量
 * @param components 数据组件谓词
 */
public record ItemIngredientPredicate(
    Optional<HolderSet<Item>> items,
    int count,
    DataComponentMatchers components
) implements IItemStackPredicate {
    /**
     * ItemIngredientPredicate编解码器
     */
    public static final Codec<ItemIngredientPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemIngredientPredicate::items),
        Codec.INT.optionalFieldOf("count", 1).forGetter(ItemIngredientPredicate::count),
        DataComponentMatchers.CODEC.codec().optionalFieldOf("components", DataComponentMatchers.ANY)
            .forGetter(ItemIngredientPredicate::components)
    ).apply(instance, ItemIngredientPredicate::new));

    /**
     * ItemIngredientPredicate流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemIngredientPredicate> STREAM_CODEC = StreamCodecUtil.codec2Stream(
        ItemIngredientPredicate.CODEC
    );

    /**
     * 创建一个物品构建器
     *
     * @param items 物品数组
     * @return 构建器实例
     */
    public static Builder of(ItemLike... items) {
        return new Builder().of(items);
    }

    /**
     * 创建一个标签构建器
     *
     * @param tag 物品标签
     * @return 构建器实例
     */
    public static Builder of(TagKey<Item> tag) {
        return new Builder().of(tag);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return this.testIgnoreCount(itemStack) && this.testCount(itemStack.count());
    }

    @Override
    public boolean testCount(int count) {
        return this.count <= count;
    }

    private static final Int2ObjectMap<ItemStackTemplate[]> INGREDIENT_CACHE = new Int2ObjectArrayMap<>();

    /**
     * 获取物品数组
     *
     * @return 物品数组
     */
    public ItemStackTemplate[] getItems() {
        int hash = this.hashCode();
        if (!INGREDIENT_CACHE.containsKey(hash)) {
            //noinspection deprecation
            INGREDIENT_CACHE.put(
                hash,
                this.items()
                    .map(itemSet -> itemSet.stream()
                        .map(itemHolder -> new ItemStackTemplate(itemHolder, this.count(), this.components().exact().asPatch()))
                        .toArray(ItemStackTemplate[]::new))
                    .orElse(new ItemStackTemplate[]{
                        new ItemStackTemplate(
                            Items.BARRIER.builtInRegistryHolder(),
                            this.count(),
                            this.components().exact().asPatch()
                        )
                    })
            );
        }
        return INGREDIENT_CACHE.get(hash);
    }

    /**
     * 构建器类，用于构建ItemIngredientPredicate实例
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<HolderSet<Item>> items = Optional.empty();
        private int count;
        private DataComponentMatchers components;

        /**
         * 构造一个构建器
         */
        private Builder() {
            this.count = 1;
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
         * 设置物品堆栈
         *
         * @param stack 物品堆栈
         * @return 构建器实例
         */
        public <D> Builder of(ItemStackTemplate stack) {
            Item item = stack.item().value();
            ItemStackTemplate defaultInstance = new ItemStackTemplate(item);
            this.of(item);
            for (TypedDataComponent<?> component : item.components()) {
                Object o = defaultInstance.get(component.type());
                if (o != null && o.equals(component.value())) continue;
                //noinspection unchecked
                this.hasComponents(
                    DataComponentMatchers.Builder.components()
                        .exact(
                            DataComponentExactPredicate.builder()
                                .expect((DataComponentType<D>) component.type(), (D) component.value())
                                .build()
                        )
                        .build()
                );
            }
            return this;
        }

        /**
         * 设置数量
         *
         * @param count 数量
         * @return 构建器实例
         */
        public Builder withCount(int count) {
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
         * 构建ItemIngredientPredicate实例
         *
         * @return ItemIngredientPredicate实例
         */
        public ItemIngredientPredicate build() {
            return new ItemIngredientPredicate(this.items, this.count, this.components);
        }
    }
}