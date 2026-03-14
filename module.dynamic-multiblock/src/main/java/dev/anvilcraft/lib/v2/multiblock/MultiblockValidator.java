package dev.anvilcraft.lib.v2.multiblock;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MultiblockValidator {
    private static final Direction[] DIRECTIONS = new Direction[] {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST,
    };

    public enum State {
        /**
         * （非控制器）已与控制器连接
         */
        BOUND
    }
}
