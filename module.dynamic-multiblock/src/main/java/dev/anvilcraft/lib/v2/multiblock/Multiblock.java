package dev.anvilcraft.lib.v2.multiblock;

import dev.anvilcraft.lib.v2.multiblock.controller.IMultiblockController;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

@Getter
public class Multiblock {
    private final IMultiblockController controller;
    private final BlockPos controllerPos;
    private final MultiblockDefinition definition;

    public Multiblock(IMultiblockController controller, BlockPos controllerPos, MultiblockDefinition definition) {
        this.controller = controller;
        this.controllerPos = controllerPos;
        this.definition = definition;
    }

    public Multiblock(Block controller, BlockPos controllerPos, MultiblockDefinition definition) {
        this.controller = IMultiblockController.of(controller);
        this.controllerPos = controllerPos;
        this.definition = definition;
    }
}
