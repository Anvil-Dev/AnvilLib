package dev.anvilcraft.lib.v2.multiblock.dynamic;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * 多方块检测的不可变快照，用于在工作线程中安全地执行谓词匹配。
 *
 * <p>快照在主线程上构建，包含所有必要的方块状态与（可选的）序列化 NBT 数据，
 * 因此工作线程无需访问 {@code Level}。
 *
 * @param controllerPosLong 控制器位置（{@link BlockPos#asLong()} 编码）
 * @param entries           每个检测位置对应的快照条目（位置 → 条目）
 */
public record MultiblockCheckSnapshot(
    long controllerPosLong,
    Map<BlockPos, Entry> entries
) {

    /**
     * 在工作线程上执行全部谓词测试。
     *
     * @return 所有位置均通过测试则返回 {@code true}
     */
    public boolean test() {
        for (Entry entry : entries.values()) {
            if (!entry.predicate().testOffThread(entry.blockState(), entry.entityNbt())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 单个位置的快照数据。
     *
     * @param blockState 该位置的方块状态
     * @param entityNbt  预序列化的方块实体 NBT（若谓词不依赖 NBT 或该位置无方块实体则为 {@code null}）
     * @param predicate  应用于此位置的谓词
     */
    public record Entry(
        BlockState blockState,
        @Nullable CompoundTag entityNbt,
        BlockStatePredicate predicate
    ) {
    }
}

