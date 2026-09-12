package dev.anvilcraft.lib.v2.multiblock.dynamic.controller;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record ControllerInfo(Block block, ResourceLocation definitionId) {
}
