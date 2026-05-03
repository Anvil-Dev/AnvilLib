package dev.anvilcraft.lib.v2.recipe.predicate.item;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipePredicateTypes;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.function.IPredicateFunction;
import dev.anvilcraft.lib.v2.recipe.predicate.function.SaveComponentToTag;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import lombok.Getter;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 物品原料条件谓词
 * <p>
 * 用于检查指定区域内是否存在特定物品原料的谓词条件，并在配方完成后消耗这些物品
 * </p>
 */
@Getter
public class HasItemIngredient extends HasItemBase<HasItemIngredient, ItemIngredientPredicate> {
    /**
     * 构造一个物品原料条件谓词
     *
     * @param offset 偏移量
     * @param range  范围
     * @param item   物品原料谓词
     */
    public HasItemIngredient(Vec3 offset, Vec3 range, ItemIngredientPredicate item, List<IPredicateFunction<?>> functions) {
        super(offset, range, item, functions);
    }

    /**
     * 转换为HasItemIngredient谓词
     *
     * @param offset 偏移量
     * @param range  范围
     * @return HasItemIngredient谓词
     */
    public static HasItemIngredient fromPredicate(ItemIngredientPredicate predicate, Vec3 offset, Vec3 range) {
        return new HasItemIngredient(offset, range, predicate, List.of());
    }

    @Override
    public IRecipePredicate.Type<HasItemIngredient> getType() {
        return LibRecipePredicateTypes.HAS_ITEM_INGREDIENT.get();
    }

    @Override
    public void snapshot(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        input.shrink(this.item.count());
    }

    @Override
    public void rollback(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        input.rollbackShrink();
    }

    @Override
    public void clearStack(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        input.clearStack();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        super.accept(context);
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
    }

    /**
     * 创建一个构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * HasItemIngredient的类型
     */
    public static class Type extends AbstractType<HasItemIngredient, ItemIngredientPredicate> {
        @Override
        protected HasItemIngredient create(Vec3 offset, Vec3 range, ItemIngredientPredicate item, List<IPredicateFunction<?>> functions) {
            return new HasItemIngredient(offset, range, item, functions);
        }

        @Override
        protected ItemIngredientPredicate decodeItem(RegistryFriendlyByteBuf buf) {
            return ItemIngredientPredicate.STREAM_CODEC.decode(buf);
        }

        @Override
        protected void encodeItem(RegistryFriendlyByteBuf buf, ItemIngredientPredicate item) {
            ItemIngredientPredicate.STREAM_CODEC.encode(buf, item);
        }

        @Override
        protected RecordCodecBuilder<HasItemIngredient, ItemIngredientPredicate> itemCodec() {
            return ItemIngredientPredicate.CODEC.fieldOf("item").forGetter(HasItemIngredient::getItem);
        }

        @Override
        public boolean conflict() {
            return true;
        }
    }

    /**
     * 构建器类，用于构建HasItemIngredient实例
     */
    public static class Builder {
        private Vec3 offset = Vec3.ZERO;
        private Vec3 range = new Vec3(1.0, 1.0, 1.0);
        private final ItemIngredientPredicate.Builder item = ItemIngredientPredicate.Builder.item();
        private final List<IPredicateFunction<?>> functions = new ArrayList<>();

        /**
         * 设置检测范围
         *
         * @param range 范围
         * @return 构建器实例
         */
        public Builder range(Vec3 range) {
            this.range = range;
            return this;
        }

        /**
         * 设置检测范围
         *
         * @param x X轴范围
         * @param y Y轴范围
         * @param z Z轴范围
         * @return 构建器实例
         */
        public Builder range(double x, double y, double z) {
            this.range = new Vec3(x, y, z);
            return this;
        }

        /**
         * 设置检测范围（各轴相同）
         *
         * @param range 范围
         * @return 构建器实例
         */
        public Builder range(double range) {
            this.range = new Vec3(range, range, range);
            return this;
        }

        /**
         * 设置物品
         *
         * @param items 物品数组
         * @return 构建器实例
         */
        public Builder of(ItemLike... items) {
            this.item.of(items);
            return this;
        }

        /**
         * 设置物品标签
         *
         * @param tag 物品标签
         * @return 构建器实例
         */
        public Builder of(TagKey<Item> tag) {
            this.item.of(tag);
            return this;
        }

        /**
         * 设置数量
         *
         * @param count 数量
         * @return 构建器实例
         */
        public Builder count(int count) {
            this.item.withCount(count);
            return this;
        }

        /**
         * 设置数据组件谓词
         *
         * @param components 数据组件谓词
         * @return 构建器实例
         */
        public Builder has(DataComponentMatchers components) {
            this.item.hasComponents(components);
            return this;
        }

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
         * @param x X坐标偏移
         * @param y Y坐标偏移
         * @param z Z坐标偏移
         * @return 构建器实例
         */
        public Builder offset(double x, double y, double z) {
            return this.offset(new Vec3(x, y, z));
        }

        /**
         * 设置向下偏移
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
         * 设置向上偏移
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

        public Builder function(IPredicateFunction<?>... function) {
            this.functions.addAll(Arrays.asList(function));
            return this;
        }

        public Builder saveComponent(DataComponentType<?> component, Identifier path) {
            return this.function(new SaveComponentToTag<>(component, path));
        }

        /**
         * 构建HasItemIngredient实例
         *
         * @return HasItemIngredient实例
         */
        public HasItemIngredient build() {
            return new HasItemIngredient(offset, range, item.build(), functions);
        }
    }
}