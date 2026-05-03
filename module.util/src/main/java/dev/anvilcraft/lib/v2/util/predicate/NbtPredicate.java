package dev.anvilcraft.lib.v2.util.predicate;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * NBT谓词
 * <p>
 * 用于匹配NBT数据的谓词，支持物品堆栈和实体的NBT数据匹配
 * </p>
 *
 * @param tag NBT标签
 */
@Slf4j
public record NbtPredicate(CompoundTag tag) implements Predicate<Tag> {
    /**
     * NbtPredicate编解码器
     */
    public static final Codec<NbtPredicate> CODEC = TagParser.LENIENT_CODEC.xmap(NbtPredicate::new, NbtPredicate::tag);

    /**
     * NbtPredicate流编解码器
     */
    public static final StreamCodec<ByteBuf, NbtPredicate> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(
        NbtPredicate::new,
        NbtPredicate::tag
    );

    /**
     * 构造一个NBT谓词
     *
     * @param tag NBT标签
     */
    public NbtPredicate {
    }

    @Override
    public boolean test(@Nullable Tag tag) {
        return tag != null && NbtUtils.compareNbt(this.tag, tag, true);
    }

    /**
     * 测试物品堆栈的NBT数据是否匹配
     *
     * @param stack 物品堆栈
     * @return 是否匹配
     */
    public boolean test(ItemStackTemplate stack) {
        CustomData customdata = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customdata.matchedBy(this.tag);
    }

    /**
     * 测试实体的NBT数据是否匹配
     *
     * @param entity 实体
     * @return 是否匹配
     */
    public boolean test(Entity entity) {
        return this.test(getEntityTagToCompare(entity));
    }

    /**
     * 获取实体用于比较的NBT标签
     *
     * @param entity 实体
     * @return NBT标签
     */
    public static CompoundTag getEntityTagToCompare(Entity entity) {
        CompoundTag compoundtag;
        try (
            ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), log)
        ) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.saveWithoutId(output);
            compoundtag = output.buildResult();
            if (entity instanceof Player) {
                ItemStack itemStack = ((Player) entity).getInventory().getSelectedItem();
                if (!itemStack.isEmpty()) {
                    compoundtag.put("SelectedItem", ItemStack.CODEC.encode(itemStack, NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
                }
            }
        }
        return compoundtag;
    }
}