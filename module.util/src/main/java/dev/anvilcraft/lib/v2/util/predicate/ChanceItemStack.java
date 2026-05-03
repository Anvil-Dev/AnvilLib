package dev.anvilcraft.lib.v2.util.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.Map;
import java.util.Optional;

/**
 * 表示一个带概率的物品堆栈
 * <p>
 * 该类用于定义在配方中可能出现的物品结果，包含物品堆栈和数量/概率信息
 * </p>
 *
 * @param stack 物品堆栈
 * @param count 数量提供器（可以是固定值或概率分布）
 */
public record ChanceItemStack(ItemStackTemplate stack, NumberProvider count) {
    /**
     * 构造一个带概率的物品堆栈
     *
     * @param stack 物品堆栈
     * @param count 数量提供器
     */
    public ChanceItemStack {
    }

    /**
     * 构造一个带概率的物品堆栈
     *
     * @param item       物品持有者
     * @param components 数据组件补丁
     * @param count      数量提供器
     */
    private ChanceItemStack(Holder<Item> item, DataComponentPatch components, NumberProvider count) {
        this(new ItemStackTemplate(item, 1, components), count);
    }

    /**
     * 创建一个带数量提供器的ChanceItemStack
     *
     * @param item   物品
     * @param amount 数量提供器
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemLike item, NumberProvider amount) {
        return new ChanceItemStack(new ItemStackTemplate(item.asItem(), 1), amount);
    }

    /**
     * 创建一个带固定数量的ChanceItemStack
     *
     * @param item  物品
     * @param count 数量
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemLike item, int count) {
        return new ChanceItemStack(new ItemStackTemplate(item.asItem(), 1), ConstantValue.exactly(count));
    }

    /**
     * 创建一个带数量提供器的ChanceItemStack
     *
     * @param stack  物品堆栈
     * @param amount 数量提供器
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemStackTemplate stack, NumberProvider amount) {
        return new ChanceItemStack(stack, amount);
    }

    /**
     * 创建一个带固定数量的ChanceItemStack
     *
     * @param stack 物品堆栈
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemStackTemplate stack) {
        return new ChanceItemStack(stack, ConstantValue.exactly(stack.count()));
    }

    /**
     * 创建一个带固定数量的ChanceItemStack
     *
     * @param stack 物品堆栈
     * @param count 数量
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemStackTemplate stack, int count) {
        return new ChanceItemStack(stack, ConstantValue.exactly(count));
    }

    /**
     * 创建一个带二项分布概率的ChanceItemStack
     *
     * @param stack  物品堆栈
     * @param count  数量
     * @param chance 概率
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemStackTemplate stack, int count, float chance) {
        return new ChanceItemStack(stack, BinomialDistributionGenerator.binomial(count, chance));
    }

    /**
     * 创建一个带二项分布概率的ChanceItemStack
     *
     * @param stack  物品堆栈
     * @param chance 概率
     * @return ChanceItemStack实例
     */
    public static ChanceItemStack of(ItemStackTemplate stack, float chance) {
        return new ChanceItemStack(stack, BinomialDistributionGenerator.binomial(stack.count(), chance));
    }

    /**
     * ChanceItemStack的编解码器
     */
    public static final Codec<ChanceItemStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Item.CODEC.fieldOf("id").forGetter(ChanceItemStack::getItemHolder),
        DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ChanceItemStack::getComponentsPatch),
        CodecUtil.NUMBER_PROVIDER.optionalFieldOf("count", ConstantValue.exactly(1.0f)).forGetter(ChanceItemStack::count)
    ).apply(instance, ChanceItemStack::new));

    /**
     * ChanceItemStack的网络流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceItemStack> STREAM_CODEC = StreamCodec.composite(
        ItemStackTemplate.STREAM_CODEC,
        ChanceItemStack::stack,
        StreamCodecUtil.NUMBER_PROVIDER,
        ChanceItemStack::count,
        ChanceItemStack::new
    );

    /**
     * 获取物品
     *
     * @return 物品
     */
    public Item getItem() {
        return this.stack.item().value();
    }

    /**
     * 获取物品持有者
     *
     * @return 物品持有者
     */
    public Holder<Item> getItemHolder() {
        return this.stack.typeHolder();
    }

    /**
     * 获取最大数量
     *
     * @return 最大数量
     */
    public int getMaxCount() {
        return (int) Math.round(NumberProviderUtil.expected(this.count));
    }

    /**
     * 获取数据组件补丁
     *
     * @return 数据组件补丁
     */
    public DataComponentPatch getComponentsPatch() {
       return this.stack.components();
    }

    public ItemStackTemplate getResult(ServerLevel level) {
        LootContext context = new LootContext.Builder(
            new LootParams(
                level,
                new ContextMap.Builder().create(new ContextKeySet.Builder().build()),
                Map.of(),
                0
            )
        ).create(Optional.empty());
        return this.stack().withCount(Math.clamp(this.count().getInt(context), 1, 99));
    }
}