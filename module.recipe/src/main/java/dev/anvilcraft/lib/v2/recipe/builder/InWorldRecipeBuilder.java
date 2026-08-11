package dev.anvilcraft.lib.v2.recipe.builder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.outcome.ChooseOneOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.SetBlock;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlock;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockIngredient;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItem;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import lombok.EqualsAndHashCode;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 世界内配方构建器
 * <p>
 * 用于构建在世界中使用的铁砧配方，支持各种触发器、谓词和结果。
 * 可以设置配方的触发条件、前置条件、冲突条件和结果操作等。
 * </p>
 */
@EqualsAndHashCode
@SuppressWarnings("unused")
public class InWorldRecipeBuilder<T extends InWorldRecipeBuilder<T>> implements RecipeBuilder {
    /**
     * 配方图标
     */
    protected final NonNullList<ItemStack> icon = NonNullList.withSize(1, Items.ANVIL.getDefaultInstance());
    /**
     * 偏移量
     */
    protected Vec3 offset = Vec3.ZERO;
    /**
     * 配方触发器
     */
    protected final @NotNull IRecipeTrigger trigger;
    /**
     * 冲突的配方谓词列表
     */
    protected final List<IRecipePredicate<?>> conflicting = new ArrayList<>();
    /**
     * 非冲突的配方谓词列表
     */
    protected final List<IRecipePredicate<?>> nonConflicting = new ArrayList<>();
    /**
     * 配方结果列表
     */
    protected final List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
    /**
     * 是否兼容
     */
    protected final boolean compatible;
    /**
     * 优先级
     */
    protected Integer priority = null;
    /**
     * 最大效率
     */
    protected int maxEfficiency = Integer.MAX_VALUE;
    /**
     * 配方组
     */
    protected String group;
    /**
     * 准则映射
     */
    protected final Map<String, Criterion<?>> criteria = Maps.newLinkedHashMap();

    /**
     * 构造一个新的世界内配方构建器
     *
     * @param trigger    配方触发器
     * @param compatible 是否兼容
     */
    protected InWorldRecipeBuilder(IRecipeTrigger trigger, boolean compatible) {
        this.trigger = trigger;
        this.compatible = compatible;
    }

    /**
     * 创建一个兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 兼容的世界内配方构建器
     */
    public static <T extends InWorldRecipeBuilder<T>> InWorldRecipeBuilder<T> compatible(IRecipeTrigger trigger) {
        return new InWorldRecipeBuilder<>(trigger, true);
    }

    /**
     * 创建一个兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 兼容的世界内配方构建器
     */
    public static <T extends InWorldRecipeBuilder<T>> InWorldRecipeBuilder<T> compatible(Supplier<IRecipeTrigger> trigger) {
        return InWorldRecipeBuilder.compatible(trigger.get());
    }

    /**
     * 创建一个不兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 不兼容的世界内配方构建器
     */
    public static <T extends InWorldRecipeBuilder<T>> InWorldRecipeBuilder<T> incompatible(IRecipeTrigger trigger) {
        return new InWorldRecipeBuilder<>(trigger, false);
    }

    /**
     * 创建一个不兼容的世界内配方构建器
     *
     * @param trigger 配方触发器
     * @return 不兼容的世界内配方构建器
     */
    public static <T extends InWorldRecipeBuilder<T>> InWorldRecipeBuilder<T> incompatible(Supplier<IRecipeTrigger> trigger) {
        return InWorldRecipeBuilder.incompatible(trigger.get());
    }

    @SuppressWarnings("unchecked")
    public T self() {
        return (T) this;
    }

    /**
     * 设置配方图标
     *
     * @param icon 配方图标物品堆
     * @return 当前构建器实例
     */
    public T icon(ItemStack icon) {
        this.icon.set(0, icon);
        return this.self();
    }

    /**
     * 添加配方谓词
     *
     * @param predicate 配方谓词
     * @return 当前构建器实例
     */
    public T with(IRecipePredicate<?> predicate) {
        if (predicate.getType().conflict()) {
            this.conflicting.add(predicate);
        } else {
            this.nonConflicting.add(predicate);
        }
        return this.self();
    }

    /**
     * 设置偏移量
     *
     * @param offset 偏移向量
     * @return 当前构建器实例
     */
    public T offset(Vec3 offset) {
        this.offset = offset;
        return this.self();
    }

    /**
     * 设置偏移量
     *
     * @param x X轴偏移量
     * @param y Y轴偏移量
     * @param z Z轴偏移量
     * @return 当前构建器实例
     */
    public T offset(double x, double y, double z) {
        return this.offset(new Vec3(x, y, z));
    }

    /**
     * 设置向下偏移量
     *
     * @param below 向下偏移距离
     * @return 当前构建器实例
     */
    public T below(double below) {
        return this.offset(Vec3.ZERO.subtract(0, below, 0));
    }

    /**
     * 设置向下偏移1格
     *
     * @return 当前构建器实例
     */
    public T below() {
        return this.below(1);
    }

    /**
     * 设置向上偏移量
     *
     * @param above 向上偏移距离
     * @return 当前构建器实例
     */
    public T above(double above) {
        return this.offset(Vec3.ZERO.add(0, above, 0));
    }

    /**
     * 设置向上偏移1格
     *
     * @return 当前构建器实例
     */
    public T above() {
        return this.above(1);
    }

    /**
     * 添加物品谓词
     *
     * @param consumer HasItem构建器消费者
     * @return 当前构建器实例
     */
    public T hasItem(Consumer<HasItem.Builder> consumer) {
        HasItem.Builder builder = HasItem.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加物品谓词
     *
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public T hasItem(ItemLike... items) {
        return this.with(HasItem.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param offset 偏移向量
     * @param items  物品列表
     * @return 当前构建器实例
     */
    public T hasItem(Vec3 offset, ItemLike... items) {
        return this.with(HasItem.builder().of(items).offset(offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public T hasItem(double x, double y, double z, ItemLike... items) {
        return this.with(HasItem.builder().of(items).offset(x, y, z).build());
    }

    /**
     * 添加物品谓词
     *
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public T hasItem(TagKey<Item> items) {
        return this.with(HasItem.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param offset 偏移向量
     * @param items  物品标签
     * @return 当前构建器实例
     */
    public T hasItem(Vec3 offset, TagKey<Item> items) {
        return this.with(HasItem.builder().offset(offset).build());
    }

    /**
     * 添加物品谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public T hasItem(double x, double y, double z, TagKey<Item> items) {
        return this.with(HasItem.builder().offset(x, y, z).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param consumer HasItemIngredient构建器消费者
     * @return 当前构建器实例
     */
    public T hasItemIngredient(Consumer<HasItemIngredient.Builder> consumer) {
        HasItemIngredient.Builder builder = HasItemIngredient.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public T hasItemIngredient(ItemLike... items) {
        return this.with(HasItemIngredient.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param offset 偏移向量
     * @param items  物品列表
     * @return 当前构建器实例
     */
    public T hasItemIngredient(Vec3 offset, ItemLike... items) {
        return this.with(HasItemIngredient.builder().of(items).offset(offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品列表
     * @return 当前构建器实例
     */
    public T hasItemIngredient(double x, double y, double z, ItemLike... items) {
        return this.with(HasItemIngredient.builder().of(items).offset(x, y, z).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public T hasItemIngredient(TagKey<Item> items) {
        return this.with(HasItemIngredient.builder().of(items).offset(this.offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param offset 偏移向量
     * @param items  物品标签
     * @return 当前构建器实例
     */
    public T hasItemIngredient(Vec3 offset, TagKey<Item> items) {
        return this.with(HasItemIngredient.builder().of(items).offset(offset).build());
    }

    /**
     * 添加物品原料谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param items 物品标签
     * @return 当前构建器实例
     */
    public T hasItemIngredient(double x, double y, double z, TagKey<Item> items) {
        return this.with(HasItemIngredient.builder().of(items).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param consumer HasBlock构建器消费者
     * @return 当前构建器实例
     */
    public T hasBlock(Consumer<HasBlock.Builder> consumer) {
        HasBlock.Builder builder = HasBlock.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加方块谓词
     *
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public T hasBlock(Block... blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public T hasBlock(Vec3 offset, Block... blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public T hasBlock(double x, double y, double z, Block... blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public T hasBlock(Collection<Block> blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public T hasBlock(Vec3 offset, Collection<Block> blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public T hasBlock(double x, double y, double z, Collection<Block> blocks) {
        return this.with(HasBlock.builder().of(blocks).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public T hasBlock(TagKey<Block> tag) {
        return this.with(HasBlock.builder().of(tag).offset(this.offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param tag    方块标签
     * @return 当前构建器实例
     */
    public T hasBlock(Vec3 offset, TagKey<Block> tag) {
        return this.with(HasBlock.builder().of(tag).offset(offset).build());
    }

    /**
     * 添加方块谓词
     *
     * @param x   X轴偏移量
     * @param y   Y轴偏移量
     * @param z   Z轴偏移量
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public T hasBlock(double x, double y, double z, TagKey<Block> tag) {
        return this.with(HasBlock.builder().of(tag).offset(x, y, z).build());
    }

    /**
     * 添加方块谓词
     *
     * @param offset 偏移向量
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public <C extends Comparable<C>> T hasBlock(Vec3 offset, BlockState state) {
        HasBlock.Builder builder = HasBlock.builder();
        Block block = state.getBlock();
        builder.of(block);
        builder.offset(offset);
        BlockState defaultState = block.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = state.getValue(property);
            Comparable<?> defaultValue = defaultState.getValue(property);
            if (value.equals(defaultValue)) continue;
            //noinspection unchecked
            builder.with((Property<C>) property, (C) value);
        }
        return this.with(builder.build());
    }

    /**
     * 添加方块谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public T hasBlock(double x, double y, double z, BlockState state) {
        return this.hasBlock(new Vec3(x, y, z), state);
    }

    /**
     * 添加方块谓词
     *
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public T hasBlock(BlockState state) {
        return this.hasBlock(this.offset, state);
    }

    /**
     * 添加方块原料谓词
     *
     * @param consumer HasBlockIngredient构建器消费者
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(Consumer<HasBlockIngredient.Builder> consumer) {
        HasBlockIngredient.Builder builder = HasBlockIngredient.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.with(builder.build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(Block... blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(Vec3 offset, Block... blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块列表
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(double x, double y, double z, Block... blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(Collection<Block> blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(this.offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(Vec3 offset, Collection<Block> blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param blocks 方块集合
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(double x, double y, double z, Collection<Block> blocks) {
        return this.with(HasBlockIngredient.builder().of(blocks).offset(new Vec3(x, y, z)).build());
    }


    /**
     * 添加方块原料谓词
     *
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(TagKey<Block> tag) {
        return this.with(HasBlockIngredient.builder().of(tag).offset(this.offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param tag    方块标签
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(Vec3 offset, TagKey<Block> tag) {
        return this.with(HasBlockIngredient.builder().of(tag).offset(offset).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x   X轴偏移量
     * @param y   Y轴偏移量
     * @param z   Z轴偏移量
     * @param tag 方块标签
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(double x, double y, double z, TagKey<Block> tag) {
        return this.with(HasBlockIngredient.builder().of(tag).offset(new Vec3(x, y, z)).build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param offset 偏移向量
     * @param state  方块状态
     * @param <C>    属性类型
     * @return 当前构建器实例
     */
    public <C extends Comparable<C>> T hasBlockIngredient(Vec3 offset, BlockState state) {
        HasBlockIngredient.Builder builder = HasBlockIngredient.builder();
        Block block = state.getBlock();
        BlockState defaultState = block.defaultBlockState();
        builder.of(block);
        builder.offset(offset);
        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = state.getValue(property);
            if (value.equals(defaultState.getValue(property))) continue;
            //noinspection unchecked
            builder.with((Property<C>) property, (C) value);
        }
        return this.with(builder.build());
    }

    /**
     * 添加方块原料谓词
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(double x, double y, double z, BlockState state) {
        return this.hasBlockIngredient(new Vec3(x, y, z), state);
    }

    /**
     * 添加方块原料谓词
     *
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public T hasBlockIngredient(BlockState state) {
        return this.hasBlockIngredient(this.offset, state);
    }

    /**
     * 添加配方结果
     *
     * @param outcome 配方结果
     * @return 当前构建器实例
     */
    public T out(IRecipeOutcome<?> outcome) {
        this.outcomes.add(outcome);
        return this.self();
    }

    /**
     * 添加选择结果
     *
     * @param consumer ChooseOneOutcome 构建器消费者
     * @return 当前构建器实例
     */
    public T chooseOne(Consumer<ChooseOneOutcome.Builder> consumer) {
        ChooseOneOutcome.Builder builder = ChooseOneOutcome.builder();
        consumer.accept(builder);
        return this.out(builder.build());
    }

    /**
     * 添加生成物品结果
     *
     * @param consumer SpawnItem 构建器消费者
     * @return 当前构建器实例
     */
    public T spawnItem(Consumer<SpawnItem.Builder> consumer) {
        SpawnItem.Builder builder = SpawnItem.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.out(builder.build());
    }

    /**
     * 添加生成物品结果
     *
     * @param offset 偏移向量
     * @param chance 生成概率
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public T spawnItem(Vec3 offset, double chance, ItemStack stack) {
        return this.out(SpawnItem.builder().offset(offset).count((float) chance).item(stack).build());
    }

    /**
     * 添加生成物品结果
     *
     * @param offset 偏移向量
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public T spawnItem(Vec3 offset, ItemStack stack) {
        return this.spawnItem(offset, 1, stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param chance 生成概率
     * @param stack  物品堆
     * @return 当前构建器实例
     */
    public T spawnItem(double x, double y, double z, double chance, ItemStack stack) {
        return this.spawnItem(new Vec3(x, y, z), chance, stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param stack 物品堆
     * @return 当前构建器实例
     */
    public T spawnItem(double x, double y, double z, ItemStack stack) {
        return this.spawnItem(new Vec3(x, y, z), stack);
    }

    /**
     * 添加生成物品结果
     *
     * @param stack 物品堆
     * @return 当前构建器实例
     */
    public T spawnItem(ItemStack stack) {
        return this.spawnItem(this.offset, stack);
    }

    /**
     * 添加设置方块结果
     *
     * @param consumer SetBlock 构建器消费者
     * @return 当前构建器实例
     */
    public T setBlock(Consumer<SetBlock.Builder> consumer) {
        SetBlock.Builder builder = SetBlock.builder();
        builder.offset(this.offset);
        consumer.accept(builder);
        return this.out(builder.build());
    }

    /**
     * 添加设置方块结果
     *
     * @param offset 偏移向量
     * @param chance 设置概率
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public T setBlock(Vec3 offset, double chance, BlockState state) {
        return this.out(SetBlock.builder().block(state).offset(offset).chance((float) chance).build());
    }

    /**
     * 添加设置方块结果
     *
     * @param offset 偏移向量
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public T setBlock(Vec3 offset, BlockState state) {
        return this.setBlock(offset, 1, state);
    }

    /**
     * 添加设置方块结果
     *
     * @param x      X轴偏移量
     * @param y      Y轴偏移量
     * @param z      Z轴偏移量
     * @param chance 设置概率
     * @param state  方块状态
     * @return 当前构建器实例
     */
    public T setBlock(double x, double y, double z, double chance, BlockState state) {
        return this.setBlock(new Vec3(x, y, z), chance, state);
    }

    /**
     * 添加设置方块结果
     *
     * @param x     X轴偏移量
     * @param y     Y轴偏移量
     * @param z     Z轴偏移量
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public T setBlock(double x, double y, double z, BlockState state) {
        return this.setBlock(new Vec3(x, y, z), state);
    }

    /**
     * 添加设置方块结果
     *
     * @param state 方块状态
     * @return 当前构建器实例
     */
    public T setBlock(BlockState state) {
        return this.setBlock(this.offset, state);
    }

    /**
     * 设置优先级
     *
     * @param priority 优先级
     * @return 当前构建器实例
     */
    public T priority(Integer priority) {
        this.priority = priority;
        return this.self();
    }

    public T maxEfficiency(int maxEfficiency) {
        this.maxEfficiency = maxEfficiency;
        return this.self();
    }

    /**
     * 构建世界内配方
     *
     * @return 世界内配方
     */
    public InWorldRecipe build() {
        return new InWorldRecipe(
            this.icon.getFirst(),
            this.trigger,
            ImmutableList.copyOf(this.conflicting),
            ImmutableList.copyOf(this.nonConflicting),
            ImmutableList.copyOf(this.outcomes),
            Objects.requireNonNullElseGet(
                this.priority,
                () -> InWorldRecipe.calcPriority(this.trigger, this.conflicting, this.nonConflicting, this.outcomes)
            ),
            this.compatible,
            this.maxEfficiency
        );
    }

    @Override
    public T unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this.self();
    }

    @Override
    public T group(@Nullable String groupName) {
        this.group = groupName;
        return this.self();
    }

    @Override
    public Item getResult() {
        return this.icon.getFirst().getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        Advancement.Builder builder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        Objects.requireNonNull(builder);
        this.criteria.forEach(builder::addCriterion);
        InWorldRecipe recipe = this.build();
        recipeOutput.accept(
            ResourceLocation.fromNamespaceAndPath(id.getNamespace(), this.group + "/" + id.getPath()),
            recipe,
            builder.build(id.withPrefix("recipes/" + this.group + "/"))
        );
    }
}
