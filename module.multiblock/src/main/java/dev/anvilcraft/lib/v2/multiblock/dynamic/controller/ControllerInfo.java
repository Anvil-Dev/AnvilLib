package dev.anvilcraft.lib.v2.multiblock.dynamic.controller;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public record ControllerInfo(Block block, Identifier definitionId) {
}
