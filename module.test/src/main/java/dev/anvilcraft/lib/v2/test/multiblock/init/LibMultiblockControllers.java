package dev.anvilcraft.lib.v2.test.multiblock.init;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.SimpleController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;

public class LibMultiblockControllers {
    public static void init() {
        ControllerRecord.register(new SimpleController(Blocks.FURNACE, LibMultiblocks.SIMPLE.location()) {
            @Override
            public void onFormed(Level level, MultiblockState state) {
                if (level.isClientSide) return;
                BlockPos pos = state.getControllerPos();
                Direction facing = level.getBlockState(pos).getValue(FurnaceBlock.FACING);
                Block.popResourceFromFace(level, pos, facing, Items.IRON_INGOT.getDefaultInstance().copyWithCount(64));
            }

            @Override
            public void onUnformed(Level level, MultiblockState state) {
                if (level.isClientSide) return;
                BlockPos pos = state.getControllerPos();
                Direction facing = level.getBlockState(pos).getValue(FurnaceBlock.FACING);
                Block.popResourceFromFace(level, pos, facing, Items.IRON_BARS.getDefaultInstance().copyWithCount(64));
            }
        });
    }
}
