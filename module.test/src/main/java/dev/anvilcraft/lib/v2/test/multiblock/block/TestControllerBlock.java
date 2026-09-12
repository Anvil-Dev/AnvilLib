package dev.anvilcraft.lib.v2.test.multiblock.block;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.test.multiblock.init.LibMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class TestControllerBlock extends Block implements IController {
    public TestControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public ResourceLocation getDefinitionId() {
        return LibMultiblocks.COMPLICATED.location();
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        if (level.isClientSide) return;
        BlockPos pos = state.getControllerPos();
        Block.popResourceFromFace(level, pos, Direction.UP, Items.IRON_INGOT.getDefaultInstance().copyWithCount(64));
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        if (level.isClientSide) return;
        BlockPos pos = state.getControllerPos();
        Block.popResourceFromFace(level, pos, Direction.UP, Items.IRON_BARS.getDefaultInstance().copyWithCount(64));
    }
}
