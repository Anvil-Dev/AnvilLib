package dev.anvilcraft.lib.v2.recipe.outcome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.init.reicpe.LibRecipeOutcomeTypes;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 多选一Outcome：多个Outcome中选择一个进行产出
 * 它本身也是一个Outcome
 *
 * @param chance  整个ChooseOneOutcome发生的概率
 * @param choices 多选一中的所有选项列表
 */
public record ChooseOneOutcome(NumberProvider chance, List<Choice> choices) implements IRecipeOutcome<ChooseOneOutcome> {
    /**
     * 多选一Outcome的构造器
     * 请使用builder类
     *
     * @param chance  整个ChooseOneOutcome发生的概率
     * @param choices 多选一中的所有选项列表
     */
    public ChooseOneOutcome(NumberProvider chance, @Unmodifiable List<Choice> choices) {
        this.chance = chance;
        this.choices = choices;
    }

    /**
     * 获取outcome类型
     *
     * @return outcome的类型
     */
    @Override
    public Type getType() {
        return LibRecipeOutcomeTypes.CHOOSE_ONE.get();
    }

    /**
     * 接受上下文，然后选择各个选项之一来执行
     *
     * @param context 配方上下文
     */
    @Override
    public void accept(InWorldRecipeContext context) {
        /*
        工作原理

        choices 列表
        每个元素有一个 weight（权重）和一个 outcome（结果）。

        构建前缀和数组 checkpoints
        checkpoints[i] = 从第一个到第 i 个的权重和。
        比如权重是 [2, 3, 5]，那 checkpoints = [2, 5, 10]，总和是 10。

        随机选择
        r * weightSum 会落在 [0, weightSum] 之间。
        用它去和 checkpoints 对比，找到第一个大于等于它的下标，就对应选中的 outcome。

        执行 outcome
        调用 choices.get(i).outcome.acceptWithChance(context)。
         */
        float weightSum = 0f;
        float[] checkpoints = new float[this.choices.size()];

        for (int i = 0; i < this.choices.size(); i++) {
            weightSum += context.getFloat(this.choices.get(i).weight());
            checkpoints[i] = weightSum;
        }

        float r = context.getLevel().getRandom().nextFloat();
        for (int i = 0; i < checkpoints.length; i++) {
            if (r * weightSum <= checkpoints[i]) {
                this.choices.get(i).outcome().acceptWithChance(context);
                return;
            }
        }
    }

    /**
     * 快速获取构造器
     *
     * @return 构造器实例对象
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 快速构造一个选项
     *
     * @param outcome 选项的产出
     * @param weight  选项的权重
     * @return 选项实例对象
     */
    public static Choice quickChoice(IRecipeOutcome<?> outcome, NumberProvider weight) {
        return new Choice(outcome, weight);
    }

    /**
     * 多选产出一中的选项
     *
     * @param outcome 每个选项的产出
     * @param weight  每个选项的权重，概率为 权重/权重总和
     */
    public record Choice(IRecipeOutcome<?> outcome, NumberProvider weight) {
        /**
         * 编解码器
         */
        public static Codec<Choice> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                IRecipeOutcome.CODEC
                    .fieldOf("outcome")
                    .forGetter(Choice::outcome),
                NumberProviders.CODEC
                    .fieldOf("weight")
                    .forGetter(Choice::weight)
            )
            .apply(ins, Choice::new));
    }

    public static class Builder {

        /**
         * （builder的）整个ChooseOneOutcome的概率
         */
        private NumberProvider chance;

        /**
         * （构建器中的）各个选项
         */
        private final List<Choice> choices;

        /**
         * 构建器
         */
        public Builder() {
            chance = ConstantValue.exactly(1f);
            choices = new ArrayList<>();
        }

        /**
         * 加入一个选项
         *
         * @param choice 被添加的选项
         * @return builder自身
         */
        public Builder choice(Choice choice) {
            choices.add(choice);
            return this;
        }

        /**
         * 快速构建并加入一个选项
         *
         * @param outcome 该选项的outcome
         * @param weight  该选项的权重
         * @return builder自身
         */
        public Builder choice(IRecipeOutcome<?> outcome, NumberProvider weight) {
            return this.choice(ChooseOneOutcome.quickChoice(outcome, weight));
        }

        /**
         * 快速构建并加入一个选项
         *
         * @param outcome 该选项的outcome
         * @param weight  该选项的权重
         * @return builder自身
         */
        public Builder choice(IRecipeOutcome<?> outcome, float weight) {
            return this.choice(outcome, ConstantValue.exactly(weight));
        }

        /**
         * 设置自身的概率
         *
         * @param provider 数字提供器格式的概率
         * @return builder自身
         */
        public Builder chance(NumberProvider provider) {
            chance = provider;
            return this;
        }

        /**
         * 设置自身的概率
         *
         * @param value 浮点数格式的概率
         * @return builder自身
         */
        public Builder chance(float value) {
            chance = ConstantValue.exactly(value);
            return this;
        }

        /**
         * 构建ChooseOneOutcome
         *
         * @return 完成的多选一outcome
         */
        public ChooseOneOutcome build() {
            return new ChooseOneOutcome(chance, choices);
        }
    }

    /**
     * Outcome的类型
     * 包含编解码器
     */
    public static class Type implements IRecipeOutcome.Type<ChooseOneOutcome> {
        /**
         * 映射编解码器
         */
        public static final MapCodec<ChooseOneOutcome> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            NumberProviders.CODEC.optionalFieldOf(
                "chance",
                ConstantValue.exactly(1f)
            ).forGetter(ChooseOneOutcome::chance),
            Choice.CODEC.listOf().fieldOf("choices").forGetter(ChooseOneOutcome::choices)
        ).apply(ins, ChooseOneOutcome::new));

        /**
         * 编解码器
         */
        public static final Codec<ChooseOneOutcome> CODEC = MAP_CODEC.codec();

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, ChooseOneOutcome> STREAM_CODEC = StreamCodecUtil.codec2Stream(Type.CODEC);

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public MapCodec<ChooseOneOutcome> codec() {
            return Type.MAP_CODEC;
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChooseOneOutcome> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }

}
