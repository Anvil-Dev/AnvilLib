package dev.anvilcraft.lib.v2.multiblock.dynamic.controller;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 多方块控制器
 */
public interface IController {
    Block getBlock();

    ResourceLocation getDefinitionId();

    default void onFormed(Level level, MultiblockState state) {
    }

    default void onUnformed(Level level, MultiblockState state) {
    }
}
