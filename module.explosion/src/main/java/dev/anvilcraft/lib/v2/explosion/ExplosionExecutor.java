package dev.anvilcraft.lib.v2.explosion;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/// 这是一个可灵活配置的入口点，用于模拟球形爆炸效果：爆炸从中心开始，逐个方向撞击并破坏方块（每 tick 处理 N 个方块）。
///
/// ### 用法：
/// ```
/// new ExplosionExecutor()
///     .radius(50)
///     .maxBreakPreTick(64)
///     .probabilityRadius(160)
///     .meltingRadius(128)
///     .execute(level, center);
/// ```
@Setter
@Accessors(fluent = true)
public class ExplosionExecutor {
    /// 爆炸时完全破坏方块的半径
    private int radius = 100;
    /// 每个服务器 tick 中最多可以移除的块数。
    private int maxBreakPreTick = AnvilLibExplosion.CONFIG.defaultRemoveBlocksPerTick;
    /// 当方块被破坏时，是否需要掉落物品
    private boolean dropItems = false;
    /// 概率半径（以块为单位），需要大于 `radius` ，该范围内的地表方块将由近到远地被概率性地破坏，距离中心越近的概率越高，最靠近 `radius` 的概率为 `100%`，最靠近 `probabilityRadius` 的概率为 `80%`
    private int probabilityRadius = 160;
    /// 融化半径（以块为单位），需要大于 `radius` ，该范围内的地表方块将由近到远地被概率性地融化，距离中心越近的概率越高，最靠近 `radius` 的概率为 `100%`，最靠近 `meltingRadius` 的概率为 `80%`
    private int meltingRadius = 128;

    private ExplosionExecutor() {
    }

    public static ExplosionExecutor create() {
        return new ExplosionExecutor();
    }

    /// 开始进行分层球形爆炸。该爆炸会创建一个 {@link ExplosionSession} 对象，该对象会自动注册到 NeoForge 事件总线中，并从爆炸中心开始逐层（逐块）移除周围的方块。
    public void execute(ServerLevel level, BlockPos pos) {
        // Ensure probabilityRadius and meltingRadius are valid
        int actualProbabilityRadius = Math.max(this.probabilityRadius, this.radius);
        int actualMeltingRadius = Math.max(this.meltingRadius, this.radius);

        new ExplosionSession(
            level,
            pos,
            this.radius,
            this.maxBreakPreTick,
            this.dropItems,
            actualProbabilityRadius,
            actualMeltingRadius
        ).start();
    }
}
