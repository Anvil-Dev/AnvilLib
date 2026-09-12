package dev.anvilcraft.lib.v2.recipe.cache;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * 方块缓存类，用于在配方执行过程中缓存和模拟方块状态变化
 * 该类提供了对方块状态的读取、修改和提交功能，确保配方执行过程中的数据一致性
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class BlockCache {
    /**
     * 方块缓存的数据键
     */
    public static final InWorldRecipeData<BlockCache> BLOCK_CACHE = InWorldRecipeData.of(AnvilLibRecipe.of("block_cache"), BlockCache::of);

    /**
     * 默认接受者
     */
    public static final Consumer<InWorldRecipeContext> DEFAULT_ACCEPTOR = (ctx) -> ctx.get(BlockCache.BLOCK_CACHE).accept();

    /**
     * 模拟的方块状态映射表
     */
    private final HashMap<BlockPos, BlockState> simulated = new HashMap<>();

    /**
     * 模拟的方块实体映射表
     */
    private final HashMap<BlockPos, BlockEntity> simulatedEntity = new HashMap<>();

    /**
     * 缓存的方块状态映射表
     */
    private final HashMap<BlockPos, BlockState> cache = new HashMap<>();

    /**
     * 缓存的方块实体映射表
     */
    private final HashMap<BlockPos, BlockEntity> cacheEntity = new HashMap<>();

    private final Set<BlockPos> deferredNeighborUpdates = new HashSet<>();

    /**
     * 世界访问器
     */
    private final LevelAccessor level;

    /**
     * 构造一个新的方块缓存
     *
     * @param level 世界访问器
     */
    public BlockCache(LevelAccessor level) {
        this.level = level;
    }

    /**
     * 创建一个新的方块缓存实例
     *
     * @param level 配方上下文
     * @param key   方块缓存数据键
     * @return 方块缓存实例
     */
    private static BlockCache of(InWorldRecipeContext level, InWorldRecipeData<BlockCache> key) {
        return new BlockCache(level.getLevel());
    }

    /**
     * 获取指定位置的方块状态
     *
     * @param pos 方块位置
     * @return 方块状态
     */
    public BlockState getBlockState(BlockPos pos) {
        this.cache.computeIfAbsent(pos, level::getBlockState);
        this.cacheEntity.computeIfAbsent(pos, level::getBlockEntity);
        this.simulatedEntity.computeIfAbsent(pos, level::getBlockEntity);
        return this.simulated.computeIfAbsent(pos, level::getBlockState);
    }

    /**
     * 获取指定位置的方块实体
     *
     * @param pos 方块位置
     * @return 方块实体
     */
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        this.cache.computeIfAbsent(pos, level::getBlockState);
        this.cacheEntity.computeIfAbsent(pos, level::getBlockEntity);
        this.simulated.computeIfAbsent(pos, level::getBlockState);
        return this.simulatedEntity.computeIfAbsent(pos, level::getBlockEntity);
    }

    /**
     * 设置指定位置的方块状态
     *
     * @param pos   方块位置
     * @param state 方块状态
     */
    public void setBlock(BlockPos pos, @Nullable BlockState state) {
        if (state == null) state = Blocks.AIR.defaultBlockState();
        BlockState oldState = this.getBlockState(pos);
        if (oldState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf oldHalf = oldState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = oldHalf == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState oldOtherState = this.getBlockState(otherPos);
            DoubleBlockHalf otherHalf = oldHalf == DoubleBlockHalf.LOWER
                                        ? DoubleBlockHalf.UPPER
                                        : DoubleBlockHalf.LOWER;
            boolean isMatchingOtherHalf = oldOtherState.is(oldState.getBlock())
                                          && oldOtherState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                                          && oldOtherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == otherHalf;
            if (isMatchingOtherHalf) {
                this.deferredNeighborUpdates.add(pos);
                this.deferredNeighborUpdates.add(otherPos);
                if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                    BlockState firstState = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, oldHalf);
                    BlockState secondState = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, otherHalf);
                    this.setSingleBlock(pos, firstState);
                    this.setSingleBlock(otherPos, secondState);
                } else {
                    this.setSingleBlock(pos, state);
                    this.setSingleBlock(otherPos, Blocks.AIR.defaultBlockState());
                }
                return;
            }
        }
        this.setSingleBlock(pos, state);
    }

    private void setSingleBlock(BlockPos pos, BlockState state) {
        this.getBlockState(pos);
        this.simulated.put(pos, state);
        if (state.getBlock() instanceof EntityBlock entityBlock) {
            this.simulatedEntity.put(pos, entityBlock.newBlockEntity(pos, state));
        } else {
            this.simulatedEntity.put(pos, null);
        }
    }

    /**
     * 设置指定位置的方块
     *
     * @param pos   方块位置
     * @param block 方块
     */
    public void setBlock(BlockPos pos, @Nullable Block block) {
        if (block == null) block = Blocks.AIR;
        this.setBlock(pos, block.defaultBlockState());
    }

    /**
     * 设置指定位置的方块实体
     *
     * @param pos    方块位置
     * @param entity 方块实体
     */
    public void setBlockEntity(BlockPos pos, @Nullable BlockEntity entity) {
        this.getBlockEntity(pos);
        this.simulatedEntity.put(pos, entity);
    }

    /**
     * 移除指定位置的方块
     *
     * @param pos 方块位置
     */
    public void removeBlock(BlockPos pos) {
        this.setBlock(pos, Blocks.AIR);
        this.setBlockEntity(pos, null);
    }

    /**
     * 提交所有模拟的方块更改到实际世界中
     */
    public void accept() {
        List<Map.Entry<BlockPos, BlockState>> changedBlocks = new ArrayList<>();
        this.simulated.forEach((pos, state) -> {
            BlockState old = this.cache.get(pos);
            if (old == null) throw new IllegalStateException("Block at " + pos + " was not found in the cache!");
            if (state.equals(old)) return;
            boolean deferNeighborUpdate = this.deferredNeighborUpdates.contains(pos);
            this.level.setBlock(pos, state, deferNeighborUpdate ? Block.UPDATE_CLIENTS : 3);
            this.cache.put(pos, state);
            this.simulated.put(pos, state);
            if (deferNeighborUpdate) {
                changedBlocks.add(Map.entry(pos, old));
            }
        });
        this.simulatedEntity.forEach((pos, entity) -> {
            if (entity == null) return;
            BlockEntity oldEntity = this.level.getBlockEntity(pos);
            if (oldEntity == null) return;
            if (entity.equals(oldEntity)) return;
            RegistryAccess access = this.level.registryAccess();
            CompoundTag oldTag = oldEntity.saveWithFullMetadata(access);
            CompoundTag newTag = entity.saveWithFullMetadata(access);
            if (oldTag.equals(newTag)) return;
            try (
                ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(oldEntity.problemPath(), log)
            ) {
                ValueInput input = TagValueInput.create(reporter, this.level.registryAccess(), newTag);
                oldEntity.loadWithComponents(input);
            }
        });
        changedBlocks.forEach(entry -> this.level.updateNeighborsAt(entry.getKey(), entry.getValue().getBlock()));
        this.deferredNeighborUpdates.clear();
    }
}
