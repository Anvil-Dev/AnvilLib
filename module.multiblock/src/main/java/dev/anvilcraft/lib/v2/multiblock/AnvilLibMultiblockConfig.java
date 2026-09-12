package dev.anvilcraft.lib.v2.multiblock;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(name = AnvilLibMultiblock.MOD_ID)
public class AnvilLibMultiblockConfig {
    @Comment("The interval of checking unformed multiblock (in ticks)")
    @BoundedDiscrete(min = 5, max = 100)
    public int unformedMultiblockCheckInterval = 10;

    @Comment("The interval of checking formed multiblock (in ticks)")
    @BoundedDiscrete(min = 5, max = 100)
    public int formedMultiblockCheckInterval = 20;

    @Comment("Thread pool size for async multiblock checking")
    @BoundedDiscrete(min = 1, max = 16)
    public int asyncThreadPoolSize = 4;

    @Comment("Maximum number of multiblock checks submitted per tick")
    @BoundedDiscrete(min = 1, max = 512)
    public int maxChecksPerTick = 128;
}
