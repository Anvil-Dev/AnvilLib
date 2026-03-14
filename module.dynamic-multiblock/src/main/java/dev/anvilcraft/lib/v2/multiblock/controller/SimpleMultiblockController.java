package dev.anvilcraft.lib.v2.multiblock.controller;

import net.minecraft.world.level.block.Block;

public record SimpleMultiblockController(Block block) implements IMultiblockController {
}
