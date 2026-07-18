package dev.anvilcraft.lib.v2.util;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/// 不限数量的 {@link ItemStack}
@Getter
@Setter
@SuppressWarnings("unused")
public class UnlimitedItemStack implements ItemInstance, MutableDataComponentHolder {
    public static final UnlimitedItemStack EMPTY = new UnlimitedItemStack(ItemStack.EMPTY, 0);
    public static final MapCodec<UnlimitedItemStack> MAP_CODEC = MapCodec.recursive(
        "UnlimitedItemStack",
        _ -> RecordCodecBuilder.mapCodec(inst -> inst.group(
            Item.CODEC_WITH_BOUND_COMPONENTS
                .fieldOf("id")
                .forGetter(UnlimitedItemStack::typeHolder),
            ExtraCodecs.intRange(1, Integer.MAX_VALUE)
                .fieldOf("count")
                .orElse(1)
                .forGetter(UnlimitedItemStack::getCount),
            DataComponentPatch.CODEC
                .optionalFieldOf("components", DataComponentPatch.EMPTY)
                .forGetter(UnlimitedItemStack::getComponentsPatch)
        ).apply(inst, UnlimitedItemStack::new))
    );
    public static final Codec<UnlimitedItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
    public static final Codec<UnlimitedItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
        .xmap(stack -> stack.orElse(UnlimitedItemStack.EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        public UnlimitedItemStack decode(RegistryFriendlyByteBuf input) {
            int count = input.readVarInt();
            if (count <= 0) {
                return UnlimitedItemStack.EMPTY;
            }
            Holder<Item> item = Item.STREAM_CODEC.decode(input);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(input);
            return new UnlimitedItemStack(item, count, patch);
        }

        public void encode(RegistryFriendlyByteBuf output, UnlimitedItemStack stack) {
            if (stack.isEmpty()) {
                output.writeVarInt(0);
                return;
            }
            output.writeVarInt(stack.getCount());
            Item.STREAM_CODEC.encode(output, stack.typeHolder());
            DataComponentPatch.STREAM_CODEC.encode(output, stack.getComponentsPatch());
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlimitedItemStack> STREAM_CODEC = new StreamCodec<>() {
        public UnlimitedItemStack decode(RegistryFriendlyByteBuf buf) {
            UnlimitedItemStack stack = UnlimitedItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) throw new DecoderException("Empty ItemStack not allowed");
            return stack;
        }

        public void encode(RegistryFriendlyByteBuf buf, UnlimitedItemStack stack) {
            if (stack.isEmpty()) throw new EncoderException("Empty ItemStack not allowed");
            UnlimitedItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, List<UnlimitedItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));
    private ItemStack stack = ItemStack.EMPTY;
    private int count = 0;

    public UnlimitedItemStack(ItemResource resource, int count) {
        this(resource.typeHolder(), count, resource.getComponentsPatch());
    }

    public UnlimitedItemStack(ItemStack stack, int count) {
        this.count = Math.max(count, 0);
        if (this.count != 0) {
            this.setStack(stack);
        }
    }

    public UnlimitedItemStack(Holder<Item> itemHolder, int count, DataComponentPatch components) {
        this(new ItemStack(itemHolder, 1, components), count);
    }

    public UnlimitedItemStack(ItemStack stack) {
        this(stack, stack.getCount());
    }

    public boolean isEmpty() {
        return this.getStack().isEmpty() || this.count <= 0;
    }

    public UnlimitedItemStack split(int amount) {
        int realAmount = Math.min(amount, this.getCount());
        UnlimitedItemStack result = this.copyWithCount(realAmount);
        this.shrink(realAmount);
        return result;
    }

    public UnlimitedItemStack copyAndClear() {
        if (this.isEmpty()) {
            return UnlimitedItemStack.EMPTY;
        } else {
            UnlimitedItemStack result = this.copy();
            this.setCount(0);
            return result;
        }
    }

    public Item getItem() {
        return this.typeHolder().value();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Holder<Item> typeHolder() {
        return this.isEmpty() ? Items.AIR.builtInRegistryHolder() : this.getStack().typeHolder();
    }

    public boolean is(ItemLike item) {
        return this.is(item.asItem());
    }

    public boolean is(Predicate<Holder<Item>> item) {
        return item.test(this.typeHolder());
    }

    public boolean isAny(ItemLike... items) {
        for (ItemLike item : items) {
            if (this.is(item)) return true;
        }
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize(this.toStack());
    }

    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
    }

    public boolean isDamageableItem() {
        return this.getStack().isDamageableItem();
    }

    public boolean isDamaged() {
        return this.getStack().isDamaged();
    }

    public UnlimitedItemStack copy() {
        return new UnlimitedItemStack(this.getStack(), this.getCount());
    }

    public UnlimitedItemStack copyWithCount(int count) {
        return new UnlimitedItemStack(this.getStack(), count);
    }

    public UnlimitedItemStack transmuteCopy(ItemLike newItem) {
        return this.transmuteCopy(newItem, this.getCount());
    }

    public UnlimitedItemStack transmuteCopy(ItemLike newItem, int newCount) {
        return this.isEmpty() ? UnlimitedItemStack.EMPTY : this.transmuteCopyIgnoreEmpty(newItem, newCount);
    }

    @SuppressWarnings("deprecation")
    private UnlimitedItemStack transmuteCopyIgnoreEmpty(ItemLike newItem, int newCount) {
        return new UnlimitedItemStack(newItem.asItem().builtInRegistryHolder(), newCount, this.getComponentsPatch());
    }

    public boolean matches(ItemStack stack) {
        return ItemStack.matches(this.toStack(), stack);
    }

    public boolean matches(ItemStackTemplate stack) {
        return ItemStack.matches(this.toStack(), stack);
    }

    public boolean matches(UnlimitedItemStack stack) {
        if (this == stack) {
            return true;
        }
        return this.getCount() == stack.getCount() && this.isSameItemSameComponents(stack);
    }

    public boolean isSameItem(ItemResource resource) {
        return resource.is(this.getItem());
    }

    public boolean isSameItem(ItemStack stack) {
        return ItemStack.isSameItem(this.toStack(), stack);
    }

    public boolean isSameItem(ItemStackTemplate stack) {
        return ItemStack.isSameItem(this.toStack(), stack);
    }

    public boolean isSameItem(UnlimitedItemStack stack) {
        return this.is(stack.getItem());
    }

    public boolean isSameItemSameComponents(@Nullable ItemResource resource) {
        if (this.isEmpty() || resource == null) {
            return this.isEmpty() == (resource == null);
        }
        return resource.matches(this.getStack());
    }

    public boolean isSameItemSameComponents(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(this.getStack(), stack);
    }

    public boolean isSameItemSameComponents(@Nullable ItemStackTemplate stack) {
        return ItemStack.isSameItemSameComponents(this.getStack(), stack);
    }

    public boolean isSameItemSameComponents(@Nullable UnlimitedItemStack stack) {
        return stack != null && this.isSameItemSameComponents(stack.getStack());
    }

    public boolean matchesIgnoringComponents(ItemStack stack, Predicate<DataComponentType<?>> ignoredPredicate) {
        return ItemStack.matchesIgnoringComponents(this.toStack(), stack, ignoredPredicate);
    }

    public boolean matchesIgnoringComponents(UnlimitedItemStack stack, Predicate<DataComponentType<?>> ignoredPredicate) {
        return this.matchesIgnoringComponents(stack.toStack(), ignoredPredicate);
    }

    public int hashItemAndComponents() {
        return ItemStack.hashItemAndComponents(this.getStack());
    }

    public static int hashStackList(List<UnlimitedItemStack> list) {
        int i = 0;
        for (UnlimitedItemStack stack : list) {
            i = i * 31 + stack.hashItemAndComponents();
        }
        return i;
    }

    @Override
    public <T> @Nullable T set(DataComponentType<T> type, @Nullable T value) {
        return this.getStack().set(type, value);
    }

    public <T> @Nullable T set(TypedDataComponent<T> value) {
        return this.getStack().set(value);
    }

    @Override
    public <T> void copyFrom(DataComponentType<T> type, DataComponentGetter source) {
        this.getStack().copyFrom(type, source);
    }

    public void copyFrom(UnlimitedItemStack stack) {
        this.setStack(stack.getStack());
        this.setCount(stack.getCount());
    }

    @Override
    public <T, U> @Nullable T update(DataComponentType<T> type, T defaultValue, U value, BiFunction<T, U, T> combiner) {
        return this.getStack().update(type, defaultValue, value, combiner);
    }

    @Override
    public <T> @Nullable T update(DataComponentType<T> type, T defaultValue, UnaryOperator<T> function) {
        return this.getStack().update(type, defaultValue, function);
    }

    @Override
    public <T> @Nullable T remove(DataComponentType<? extends T> type) {
        return this.getStack().remove(type);
    }

    @Override
    public void applyComponents(DataComponentPatch patch) {
        this.getStack().applyComponents(patch);
    }

    @Override
    public void applyComponents(DataComponentMap components) {
        this.getStack().applyComponents(components);
    }

    @Override
    public <T extends TooltipProvider> void addToTooltip(
        DataComponentType<T> type,
        Item.TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> consumer,
        TooltipFlag flag
    ) {
        this.toStack().addToTooltip(type, context, display, consumer, flag);
    }

    @Override
    public ItemEnchantments getTagEnchantments() {
        return this.getStack().getTagEnchantments();
    }

    public int getCount() {
        return this.isEmpty() ? 0 : this.count;
    }

    @Override
    public int count() {
        return this.getCount();
    }

    public void grow(int amount) {
        this.setCount(this.getCount() + amount);
    }

    public void shrink(int amount) {
        this.grow(-amount);
    }

    /// 将内部存储的物品栈替换为指定物品栈
    ///
    /// <p>注意：该方法<b>不会</b>设置数量。请使用 {@link UnlimitedItemStack#setCount(int)} 设置数量</p>
    ///
    /// @param stack 提供物品和数据组件的 {@link ItemStack}
    ///
    /// @see UnlimitedItemStack#setCount(int)
    public void setStack(ItemStack stack) {
        this.stack = stack.copyWithCount(1);
    }

    @Override
    public DataComponentMap getComponents() {
        return this.getStack().getComponents();
    }

    public DataComponentPatch getComponentsPatch() {
        return this.getStack().getComponentsPatch();
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder() {
        return this.getStack().getCraftingRemainder();
    }

    public static boolean listMatches(List<UnlimitedItemStack> list, List<UnlimitedItemStack> other) {
        if (list.size() != other.size()) return false;
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isSameItemSameComponents(other.get(i))) return false;
        }
        return true;
    }

    /// 将本物品栈转为一个 {@link ItemStack}。<br>
    /// 数量可能大于 {@link ItemStack} 允许的最大数量。<br>
    /// 若需要数量安全的 {@link ItemStack}，请查看{@link UnlimitedItemStack#toStacks()}
    ///
    /// @return 一个与本物品栈数据完全相同的 {@link ItemStack}
    /// @see UnlimitedItemStack#toStacks()
    public ItemStack toStack() {
        return this.getStack().copyWithCount(this.getCount());
    }

    /// 将本物品栈按存储的 {@link ItemStack} 允许的最大数量转为一个物品栈列表。
    ///
    /// @return 一个物品栈列表。<br>
    /// 每个物品栈都有相同的物品和数据组件。<br>
    /// 将本物品栈的数量 {@code count} 按存储的物品栈允许的最大数量 {@code max} 分割为 {@code n} 份，<br>
    /// 前 {@code n - 1} 份物品栈的数量都为 {@code max}，<br>
    /// 最后一份物品栈的数量为 {@code count - [(n - 1) * max]}
    /// @see UnlimitedItemStack#toStack()
    public List<ItemStack> toStacks() {
        ItemStack stack = this.getStack();
        int count = this.getCount();
        int maxCount = stack.getMaxStackSize();
        if (count <= maxCount) {
            return List.of(stack.copyWithCount(count));
        }

        int fullStacks = count / maxCount;
        ImmutableList.Builder<ItemStack> stacksBuilder = ImmutableList.builder();
        for (int i = 0; i < fullStacks; i++) {
            stacksBuilder.add(stack.copyWithCount(maxCount));
        }

        int remain = count % maxCount;
        if (remain != 0) {
            stacksBuilder.add(stack.copyWithCount(remain));
        }

        return stacksBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnlimitedItemStack stack1)) return false;
        return this.matches(stack1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getStack().getItem(), this.getCount(), this.getStack().getComponents());
    }

    @Override
    public String toString() {
        return this.getCount() + " " + this.getItem();
    }
}
