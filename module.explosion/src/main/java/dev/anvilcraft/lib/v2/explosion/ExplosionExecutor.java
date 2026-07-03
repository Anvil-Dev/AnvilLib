package dev.anvilcraft.lib.v2.explosion;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.apache.logging.log4j.util.TriConsumer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
    private int probabilityRadius = 128;
    /// 融化半径（以块为单位），需要大于 `radius` ，该范围内的地表方块将由近到远地被概率性地融化，距离中心越近的概率越高，最靠近 `radius` 的概率为 `100%`，最靠近 `meltingRadius` 的概率为 `80%`
    private int meltingRadius = 160;
    /// 不允许被爆炸破坏的方块列表
    private List<Predicate<Block>> excludedBlocks = new ArrayList<>(List.of(
        block -> block.defaultDestroyTime() < 0
    ));
    /// 脆弱的方块列表，在范围内的这些方块会被完全破坏
    @SuppressWarnings("deprecation")
    private List<Predicate<Block>> frangibleBlocks = new ArrayList<>(List.of(
        block -> block.builtInRegistryHolder().is(Tags.Blocks.GLASS_BLOCKS),
        block -> block.builtInRegistryHolder().is(Tags.Blocks.GLASS_PANES),
        block -> block.builtInRegistryHolder().is(BlockTags.LEAVES),
        block -> block.builtInRegistryHolder().is(BlockTags.REPLACEABLE)
    ));
    private @Nullable Entity executor = null;
    /// 方块破坏时触发的实体处理函数
    private @Nullable TriConsumer<ServerLevel, BlockPos, Entity> entityProcessor = (level, _, entity) -> entity.hurtServer(
        level,
        entity.damageSources().explosion(null, ExplosionExecutor.this.executor),
        ExplosionExecutor.this.radius * 0.5f
    );

    private ExplosionExecutor() {
    }

    public static ExplosionExecutor create() {
        return new ExplosionExecutor();
    }

    @SafeVarargs
    public final ExplosionExecutor excludedBlocks(Predicate<Block>... blocks) {
        this.excludedBlocks.addAll(List.of(blocks));
        return this;
    }

    public final ExplosionExecutor excludedBlocks(Block... blocks) {
        for (Block block : blocks) {
            this.excludedBlocks.add(block1 -> block1 == block);
        }
        return this;
    }

    @SafeVarargs
    @SuppressWarnings("deprecation")
    public final ExplosionExecutor excludedBlocks(TagKey<Block>... blocks) {
        for (TagKey<Block> block : blocks) {
            this.excludedBlocks.add(block1 -> block1.builtInRegistryHolder().is(block));
        }
        return this;
    }

    @SafeVarargs
    public final ExplosionExecutor frangibleBlocks(Predicate<Block>... blocks) {
        this.frangibleBlocks.addAll(List.of(blocks));
        return this;
    }

    public final ExplosionExecutor frangibleBlocks(Block... blocks) {
        for (Block block : blocks) {
            this.frangibleBlocks.add(block1 -> block1 == block);
        }
        return this;
    }

    @SafeVarargs
    @SuppressWarnings("deprecation")
    public final ExplosionExecutor frangibleBlocks(TagKey<Block>... blocks) {
        for (TagKey<Block> block : blocks) {
            this.frangibleBlocks.add(block1 -> block1.builtInRegistryHolder().is(block));
        }
        return this;
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
            Math.min(this.maxBreakPreTick, AnvilLibExplosion.CONFIG.maxRemoveBlocksPerTick),
            this.dropItems,
            actualProbabilityRadius,
            actualMeltingRadius,
            this.excludedBlocks,
            this.frangibleBlocks,
            this.entityProcessor
        ).start();
    }
}
