package dev.anvilcraft.lib.v2.test.mixin;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import net.minecraft.world.level.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBlock.class)
abstract class ChestBlockMixin implements IMoveableEntityBlock {
}
