package dev.anvilcraft.lib.v2.explosion;

import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(name = AnvilLibExplosion.MOD_ID, group = "anvillib")
public class AnvilLibExplosionConfig {
    @Comment("The maximum number of blocks that can be deleted per tick.")
    public int maxRemoveBlocksPerTick = 2048;
    @Comment("The default number of blocks that can be deleted per tick.")
    public int defaultRemoveBlocksPerTick = 128;
}
