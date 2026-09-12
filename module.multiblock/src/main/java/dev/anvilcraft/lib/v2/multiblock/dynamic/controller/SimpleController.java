package dev.anvilcraft.lib.v2.multiblock.dynamic.controller;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

@Getter
public abstract class SimpleController implements IController {
    private final Block block;
    private final ResourceLocation definitionId;

    protected SimpleController(Block block, ResourceLocation definitionId) {
        this.block = block;
        this.definitionId = definitionId;
    }
}
