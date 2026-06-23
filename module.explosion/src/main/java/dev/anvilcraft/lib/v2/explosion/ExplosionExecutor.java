package dev.anvilcraft.lib.v2.explosion;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Fluent-configurable entry point for a spherical explosion that breaks blocks
 * shell-by-shell from the center outward, N blocks per tick.
 *
 * <p>Usage:
 * <pre>{@code
 * new ExplosionExecutor()
 *     .radius(50)
 *     .maxBreakPreTick(64)
 *     .execute(level, center);
 * }</pre>
 */
@Setter
@Accessors(fluent = true)
public class ExplosionExecutor {
    /**
     * Maximum explosion radius in blocks (default 100).
     */
    private int radius = 100;
    /**
     * Maximum blocks to remove per server tick.
     */
    private int maxBreakPreTick = AnvilLibExplosion.CONFIG.defaultRemoveBlocksPerTick;

    private boolean dropItems = false;

    private ExplosionExecutor() {
    }

    public static ExplosionExecutor create() {
        return new ExplosionExecutor();
    }

    /**
     * Begin the layered spherical explosion. Creates a {@link ExplosionSession}
     * that self-registers on the NeoForge event bus and removes blocks
     * shell-by-shell from the center outward.
     */
    public void execute(ServerLevel level, BlockPos pos) {
        new ExplosionSession(level, pos, this.radius, this.maxBreakPreTick, this.dropItems).start();
    }
}
