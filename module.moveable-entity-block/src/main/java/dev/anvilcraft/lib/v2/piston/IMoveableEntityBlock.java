package dev.anvilcraft.lib.v2.piston;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 可移动方块实体的安置与移除回调。 */
@SuppressWarnings("unused")
public interface IMoveableEntityBlock extends EntityBlock {
    /**
     * 通知可移动方块实体已到位
     *
     * @param level 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @param be    方块实体
     */
    default void notifyMoved(Level level, BlockPos pos, BlockState state, BlockEntity be) {
    }

    /**
     * 移动中的方块无法安置时释放实体持有的内容；方块本身的战利品由调用方生成。
     *
     * <p>实体尚未安装到目标位置，禁止通过世界查询或移除目标位置的方块实体。
     * 默认释放普通容器库存，潜影盒由其方块掉落保存库存；其他存储实现可覆盖此方法。</p>
     *
     * @param level 服务端世界
     * @param pos 掉落位置
     * @param state 被移动方块的状态
     * @param be 被移动的方块实体，其世界和坐标由调用方设置
     */
    default void dropMovingContents(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        if (!level.isClientSide && be instanceof Container container && !(state.getBlock() instanceof ShulkerBoxBlock)) {
            Containers.dropContents(level, pos, container);
        }
    }
}
