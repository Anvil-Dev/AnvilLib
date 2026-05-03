package dev.anvilcraft.lib.v2.multiblock.dynamic.controller;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多方块控制器
 */
public interface IController {
    /**
     * 获取控制器方块
     *
     * @return 控制器方块
     */
    Block getBlock();

    /**
     * 获取定义 ID
     *
     * @return 定义 ID
     */
    Identifier getDefinitionId();

    /**
     * 多方块成型回调
     *
     * @param level 多方块所在的世界
     * @param state 多方块状态
     */
    default void onFormed(Level level, MultiblockState state) {
    }


    /**
     * 多方块未成型回调
     *
     * @param level 多方块所在的世界
     * @param state 多方块状态
     */
    default void onUnformed(Level level, MultiblockState state) {
    }

    /**
     * 修正控制器方块位置
     *
     * <p>一般用于多方块部件调整控制器方块位置</p>
     *
     * @param level 控制器所在的世界
     * @param pos   控制器所在的位置
     * @param state 控制器方块状态
     * @return 修正后的方块位置
     */
    default BlockPos correctPos(ServerLevel level, BlockPos pos, BlockState state) {
        return pos;
    }
}
