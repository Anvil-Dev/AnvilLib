package dev.anvilcraft.lib.v2.multiblock.part;

import dev.anvilcraft.lib.v2.multiblock.Multiblock;
import net.minecraft.world.level.LevelAccessor;

public interface IMultiPart {
    default boolean anvillib$isController() {
        throw new AssertionError();
    }

    default void anvillib$bind(Multiblock multiblock) {
        throw new AssertionError();
    }

    default boolean anvillib$isBound() {
        throw new AssertionError();
    }

    default void anvillib$unbind(LevelAccessor level) {
        throw new AssertionError();
    }
}
