package dev.anvilcraft.lib.v2.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeTypes;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import dev.anvilcraft.lib.v2.recipe.util.IPrioritized;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.ShapelessMatcher;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 世界内配方类，定义了在世界中执行的配方
 * 该类实现了 Minecraft 的 Recipe 接口，用于处理在世界中而非工作台中执行的配方
 *
 */
@EqualsAndHashCode
@ToString
public class InWorldRecipe implements Recipe<InWorldRecipeContext>, IPrioritized {
    /**
     * 配方图标
     */
    private final @Unmodifiable ItemStackTemplate icon;
    /**
     * 配方触发器
     */
    private final IRecipeTrigger trigger;
    /**
     * 配方冲突的谓词列表
     */
    private final @Unmodifiable List<IRecipePredicate<?>> conflicting;
    /**
     * 配方不冲突的谓词列表
     */
    private final @Unmodifiable List<IRecipePredicate<?>> nonConflicting;
    /**
     * 配方结果列表
     */
    private final @Unmodifiable List<IRecipeOutcome<?>> outcomes;
    /**
     * 配方优先级
     */
    private final int priority;
    /**
     * 是否兼容
     */
    private final boolean compatible;
    /**
     * 最大效率
     */
    private final int maxEfficiency;
    private PlacementInfo placementInfo;

    /**
     * 构造一个新的世界内配方
     *
     * @param icon           配方图标
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @param priority       配方优先级
     * @param compatible     是否兼容
     * @param maxEfficiency  最大效率
     */
    public InWorldRecipe(
        ItemStackTemplate icon,
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes,
        int priority,
        boolean compatible,
        int maxEfficiency
    ) {
        this.icon = icon;
        this.trigger = trigger;
        this.conflicting = conflicting;
        this.nonConflicting = nonConflicting;
        this.outcomes = outcomes;
        this.priority = priority;
        this.compatible = compatible;
        this.maxEfficiency = maxEfficiency;
    }

    /**
     * 构造一个新的世界内配方
     *
     * @param icon           配方图标
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @param priority       配方优先级
     * @param compatible     是否兼容
     */
    public InWorldRecipe(
        ItemStackTemplate icon,
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes,
        int priority,
        boolean compatible
    ) {
        this(icon, trigger, conflicting, nonConflicting, outcomes, priority, compatible, Integer.MAX_VALUE);
    }

    /**
     * 构造一个新的世界内配方，自动计算优先级
     *
     * @param icon           配方图标
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @param compatible     是否兼容
     * @param maxEfficiency  最大效率
     */
    public InWorldRecipe(
        ItemStackTemplate icon,
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes,
        boolean compatible,
        int maxEfficiency
    ) {
        this(
            icon,
            trigger,
            conflicting,
            nonConflicting,
            outcomes,
            InWorldRecipe.calcPriority(trigger, conflicting, nonConflicting, outcomes),
            compatible,
            maxEfficiency
        );
    }

    /**
     * 构造一个新的世界内配方，自动计算优先级
     *
     * @param icon           配方图标
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @param compatible     是否兼容
     */
    public InWorldRecipe(
        ItemStackTemplate icon,
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes,
        boolean compatible
    ) {
        this(
            icon,
            trigger,
            conflicting,
            nonConflicting,
            outcomes,
            InWorldRecipe.calcPriority(trigger, conflicting, nonConflicting, outcomes),
            compatible,
            Integer.MAX_VALUE
        );
    }

    /**
     * 计算配方优先级
     *
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @return 计算出的优先级值
     */
    public static int calcPriority(
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes
    ) {
        int priority = trigger.priority();
        for (IRecipePredicate<?> predicate : conflicting) {
            priority += predicate.priority();
        }
        for (IRecipePredicate<?> predicate : nonConflicting) {
            priority += predicate.priority();
        }
        for (IRecipeOutcome<?> outcome : outcomes) {
            priority += outcome.priority();
        }
        return priority;
    }

    /**
     * 判断配方是否匹配给定的上下文和世界
     *
     * @param context 配方上下文
     * @param level   世界
     * @return 是否匹配
     */
    @Override
    public boolean matches(InWorldRecipeContext context, Level level) {
        boolean nonConflicting = ShapelessMatcher.compatible(this.nonConflicting, context);
        if (!nonConflicting) {
            context.getStack().clear();
            return false;
        }
        boolean flag;
        if (this.compatible) {
            flag = ShapelessMatcher.compatible(this.conflicting, context);
        } else {
            flag = ShapelessMatcher.incompatible(this.conflicting, context);
        }
        if (!flag) {
            context.getStack().clear();
        }
        context.getStack().forEach(predicate -> predicate.clearStack(context));
        return flag;
    }

    /**
     * 组装配方结果
     *
     * @param context 配方上下文
     * @return 配方结果物品堆
     */
    @Override
    public ItemStack assemble(InWorldRecipeContext context) {
        List<IRecipePredicate<?>> stack = context.getStack();
        IRecipePredicate<?> predicate;
        while (!stack.isEmpty()) {
            predicate = stack.removeFirst();
            predicate.accept(context);
        }
        for (IRecipeOutcome<?> outcome : this.outcomes) {
            outcome.acceptWithChance(context);
        }
        return this.icon.create();
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "in_world";
    }

    /**
     * 获取配方序列化器
     *
     * @return 配方序列化器
     */
    @Override
    public RecipeSerializer<? extends InWorldRecipe> getSerializer() {
        return LibRecipeTypes.IN_WORLD_RECIPE_SERIALIZER.get();
    }

    /**
     * 获取配方类型
     *
     * @return 配方类型
     */
    @Override
    public RecipeType<? extends InWorldRecipe> getType() {
        return LibRecipeTypes.IN_WORLD_RECIPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.createFromOptionals(List.of());
        }

        return this.placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public @Unmodifiable ItemStackTemplate icon() {
        return icon;
    }

    public IRecipeTrigger trigger() {
        return trigger;
    }

    public @Unmodifiable List<IRecipePredicate<?>> conflicting() {
        return conflicting;
    }

    public @Unmodifiable List<IRecipePredicate<?>> nonConflicting() {
        return nonConflicting;
    }

    public @Unmodifiable List<IRecipeOutcome<?>> outcomes() {
        return outcomes;
    }

    @Override
    public int priority() {
        return priority;
    }

    public boolean compatible() {
        return compatible;
    }

    public int maxEfficiency() {
        return maxEfficiency;
    }

    /**
     * 世界内配方序列化器
     */
    public static class Serializer {
        private static final Codec<IRecipePredicate<?>> PREDICATE_CODEC = LibRegistries.PREDICATE_TYPE_REGISTRY.byNameCodec()
            .dispatch(IRecipePredicate::getType, IRecipePredicate.Type::codec);
        private static final Codec<IRecipeOutcome<?>> OUTCOME_CODEC = LibRegistries.OUTCOME_TYPE_REGISTRY.byNameCodec()
            .dispatch(IRecipeOutcome::getType, IRecipeOutcome.Type::codec);
        public static final MapCodec<InWorldRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStackTemplate.CODEC.fieldOf("icon").orElseGet(() -> new ItemStackTemplate(Items.ANVIL)).forGetter(InWorldRecipe::icon),
            LibRegistries.TRIGGER_REGISTRY.byNameCodec().fieldOf("trigger").forGetter(InWorldRecipe::trigger),
            PREDICATE_CODEC.listOf().fieldOf("conflicting").forGetter(InWorldRecipe::conflicting),
            PREDICATE_CODEC.listOf().fieldOf("non_conflicting").forGetter(InWorldRecipe::nonConflicting),
            OUTCOME_CODEC.listOf().fieldOf("outcomes").forGetter(InWorldRecipe::outcomes),
            Codec.INT.fieldOf("priority").orElse(1).forGetter(InWorldRecipe::priority),
            Codec.BOOL.fieldOf("compatible").orElse(true).forGetter(InWorldRecipe::compatible),
            Codec.INT.optionalFieldOf("max_efficiency", Integer.MAX_VALUE).forGetter(InWorldRecipe::maxEfficiency)
        ).apply(instance, InWorldRecipe::new));

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        public MapCodec<InWorldRecipe> codec() {
            return Serializer.CODEC;
        }

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, InWorldRecipe> STREAM_CODEC = StreamCodec.of(
            Serializer::encode,
            Serializer::decode
        );

        /**
         * 获取流编解码器
         *
         * @return 流编解码器
         */
        public StreamCodec<RegistryFriendlyByteBuf, InWorldRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }

        /**
         * 编码配方到字节缓冲区
         *
         * @param buf    字节缓冲区
         * @param recipe 要编码的配方
         * @param <P>    配方谓词类型
         * @param <O>    配方结果类型
         */
        @SuppressWarnings("unchecked")
        private static <P extends IRecipePredicate<P>, O extends IRecipeOutcome<O>> void encode(
            RegistryFriendlyByteBuf buf,
            InWorldRecipe recipe
        ) {
            ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.icon());
            buf.writeIdentifier(Objects.requireNonNull(recipe.trigger().getId()));
            buf.writeVarInt(recipe.conflicting().size());
            for (IRecipePredicate<?> predicate : recipe.conflicting()) {
                buf.writeIdentifier(Objects.requireNonNull(predicate.getType().getId()));
                ((P) predicate).getType().streamCodec().encode(buf, (P) predicate);
            }
            buf.writeVarInt(recipe.nonConflicting().size());
            for (IRecipePredicate<?> predicate : recipe.nonConflicting()) {
                buf.writeIdentifier(Objects.requireNonNull(predicate.getType().getId()));
                ((P) predicate).getType().streamCodec().encode(buf, (P) predicate);
            }
            buf.writeVarInt(recipe.outcomes().size());
            for (IRecipeOutcome<?> outcome : recipe.outcomes()) {
                buf.writeIdentifier(Objects.requireNonNull(outcome.getType().getId()));
                ((O) outcome).getType().streamCodec().encode(buf, (O) outcome);
            }
            buf.writeInt(recipe.priority());
            buf.writeBoolean(recipe.compatible());
            buf.writeInt(recipe.maxEfficiency());
        }

        /**
         * 从字节缓冲区解码配方
         *
         * @param buf 字节缓冲区
         * @return 解码出的配方
         */
        private static InWorldRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStackTemplate icon = ItemStackTemplate.STREAM_CODEC.decode(buf);
            IRecipeTrigger trigger = Objects.requireNonNull(LibRegistries.TRIGGER_REGISTRY.getValue(buf.readIdentifier()));
            List<IRecipePredicate<?>> conflicting = decodeRecipePredicateList(buf);
            List<IRecipePredicate<?>> nonConflicting = decodeRecipePredicateList(buf);
            List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
            int outcomesSize = buf.readVarInt();
            for (int i = 0; i < outcomesSize; i++) {
                Identifier location = buf.readIdentifier();
                IRecipeOutcome.Type<?> type = LibRegistries.OUTCOME_TYPE_REGISTRY.getValue(location);
                if (type == null) throw new IllegalArgumentException("Unknown outcome type: " + location);
                IRecipeOutcome<?> outcome = type.streamCodec().decode(buf);
                outcomes.add(outcome);
            }
            return new InWorldRecipe(
                icon,
                trigger,
                Collections.unmodifiableList(conflicting),
                Collections.unmodifiableList(nonConflicting),
                Collections.unmodifiableList(outcomes),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt()
            );
        }

        /**
         * 从字节缓冲区解码配方谓词列表
         *
         * @param buf 字节缓冲区
         * @return 解码出的配方谓词列表
         */
        private static List<IRecipePredicate<?>> decodeRecipePredicateList(
            RegistryFriendlyByteBuf buf
        ) {
            int size = buf.readVarInt();
            List<IRecipePredicate<?>> predicates = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Identifier location = buf.readIdentifier();
                IRecipePredicate.Type<?> type = LibRegistries.PREDICATE_TYPE_REGISTRY.getValue(location);
                if (type == null) throw new IllegalArgumentException("Unknown predicate type: " + location);
                IRecipePredicate<?> predicate = type.streamCodec().decode(buf);
                predicates.add(predicate);
            }
            return predicates;
        }
    }
}