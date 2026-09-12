package dev.anvilcraft.lib.v2.piston;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IMoveableEntityBlock extends EntityBlock {
    /// 通知可移动方块实体已到位
    ///
    /// @param level 世界
    /// @param pos   方块位置
    /// @param state 方块状态
    /// @param be    方块实体
    default void notifyMoved(Level level, BlockPos pos, BlockState state, BlockEntity be) {
    }
}
