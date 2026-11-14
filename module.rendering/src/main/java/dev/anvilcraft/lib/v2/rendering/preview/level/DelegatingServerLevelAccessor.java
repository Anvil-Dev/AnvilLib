package dev.anvilcraft.lib.v2.rendering.preview.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 将调用转发到底层 {@link LevelAccessor} 的 ServerLevelAccessor 适配器。
 *
 * <p>用于结构模板放置阶段必须提供服务端访问器接口的场景，
 * 实际执行仍发生在预览客户端关卡中。</p>
 */
public class DelegatingServerLevelAccessor implements ServerLevelAccessor {
    private final LevelAccessor delegate;

    public DelegatingServerLevelAccessor(LevelAccessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public ServerLevel getLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long nextSubTickCount() {
        return this.delegate.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.delegate.getBlockTicks();
    }

    @Override
    public void scheduleTick(BlockPos pos, Block block, int delay, TickPriority priority) {
        this.delegate.scheduleTick(pos, block, delay, priority);
    }

    @Override
    public void scheduleTick(BlockPos pos, Block block, int delay) {
        this.delegate.scheduleTick(pos, block, delay);
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.delegate.getFluidTicks();
    }

    @Override
    public void scheduleTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority) {
        this.delegate.scheduleTick(pos, fluid, delay, priority);
    }

    @Override
    public void scheduleTick(BlockPos pos, Fluid fluid, int delay) {
        this.delegate.scheduleTick(pos, fluid, delay);
    }

    @Override
    public LevelData getLevelData() {
        return this.delegate.getLevelData();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        return new DifficultyInstance(this.delegate.getDifficulty(), 0, 0, 0);
    }

    @Override
    @Nullable
    public MinecraftServer getServer() {
        return this.delegate.getServer();
    }

    @Override
    public Difficulty getDifficulty() {
        return this.delegate.getDifficulty();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.delegate.getChunkSource();
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return this.delegate.hasChunk(chunkX, chunkZ);
    }

    @Override
    public RandomSource getRandom() {
        return this.delegate.getRandom();
    }

    @Override
    public void playSound(
        @org.jspecify.annotations.Nullable Entity entity,
        BlockPos blockPos,
        SoundEvent soundEvent,
        SoundSource soundSource,
        float v,
        float v1
    ) {

    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this.delegate.addParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void levelEvent(@org.jspecify.annotations.Nullable Entity entity, int i, BlockPos blockPos, int i1) {

    }

    @Override
    public void levelEvent(int type, BlockPos pos, int data) {
        this.delegate.levelEvent(type, pos, data);
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 position, GameEvent.Context context) {
        this.delegate.gameEvent(event, position, context);
    }

    @Override
    public void gameEvent(@Nullable Entity entity, Holder<GameEvent> event, Vec3 position) {
        this.delegate.gameEvent(entity, event, position);
    }

    @Override
    public void gameEvent(@Nullable Entity entity, Holder<GameEvent> event, BlockPos pos) {
        this.delegate.gameEvent(entity, event, pos);
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, BlockPos pos, GameEvent.Context context) {
        this.delegate.gameEvent(event, pos, context);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return this.delegate.getBlockEntity(pos, type);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB collisionBox) {
        return this.delegate.getEntityCollisions(entity, collisionBox);
    }

    @Override
    public boolean isUnobstructed(@Nullable Entity entity, VoxelShape shape) {
        return this.delegate.isUnobstructed(entity, shape);
    }

    @Override
    public BlockPos getHeightmapPos(Heightmap.Types heightmapType, BlockPos pos) {
        return this.delegate.getHeightmapPos(heightmapType, pos);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB area, Predicate<? super Entity> predicate) {
        return this.delegate.getEntities(entity, area, predicate);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate) {
        return this.delegate.getEntities(entityTypeTest, bounds, predicate);
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> clazz, AABB area, Predicate<? super T> filter) {
        return this.delegate.getEntitiesOfClass(clazz, area, filter);
    }

    @Override
    public List<? extends Player> players() {
        return this.delegate.players();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB area) {
        return this.delegate.getEntities(entity, area);
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AABB area) {
        return this.delegate.getEntitiesOfClass(entityClass, area);
    }

    @Override
    @Nullable
    public Player getNearestPlayer(double x, double y, double z, double distance, @Nullable Predicate<Entity> predicate) {
        return this.delegate.getNearestPlayer(x, y, z, distance, predicate);
    }

    @Override
    @Nullable
    public Player getNearestPlayer(Entity entity, double distance) {
        return this.delegate.getNearestPlayer(entity, distance);
    }

    @Override
    @Nullable
    public Player getNearestPlayer(double x, double y, double z, double distance, boolean creativePlayers) {
        return this.delegate.getNearestPlayer(x, y, z, distance, creativePlayers);
    }

    @Override
    public boolean hasNearbyAlivePlayer(double x, double y, double z, double distance) {
        return this.delegate.hasNearbyAlivePlayer(x, y, z, distance);
    }

    @Override
    @Nullable
    public Player getPlayerByUUID(UUID uniqueId) {
        return this.delegate.getPlayerByUUID(uniqueId);
    }

    @Override
    @Nullable
    public ChunkAccess getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
        return this.delegate.getChunk(x, z, requiredStatus, nonnull);
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        return this.delegate.getHeight(heightmapType, x, z);
    }

    @Override
    public int getSkyDarken() {
        return this.delegate.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.delegate.getBiomeManager();
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        return this.delegate.getBiome(pos);
    }

    @Override
    public Stream<BlockState> getBlockStatesIfLoaded(AABB aabb) {
        return this.delegate.getBlockStatesIfLoaded(aabb);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        return this.delegate.getNoiseBiome(i, j, k);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return this.delegate.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return this.delegate.isClientSide();
    }

    @Override
    public int getSeaLevel() {
        return this.delegate.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.delegate.dimensionType();
    }

    @Override
    public int getHeight() {
        return this.delegate.getHeight();
    }

    @Override
    public boolean isEmptyBlock(BlockPos pos) {
        return this.delegate.isEmptyBlock(pos);
    }

    @Override
    public boolean canSeeSkyFromBelowWater(BlockPos pos) {
        return this.delegate.canSeeSkyFromBelowWater(pos);
    }

    @Override
    public float getPathfindingCostFromLightLevels(BlockPos blockPos) {
        return this.delegate.getPathfindingCostFromLightLevels(blockPos);
    }

    @Override
    public float getLightLevelDependentMagicValue(BlockPos blockPos) {
        return this.delegate.getLightLevelDependentMagicValue(blockPos);
    }

    @Override
    public int getDirectSignal(BlockPos pos, Direction direction) {
        return this.delegate.getDirectSignal(pos, direction);
    }

    @Override
    public ChunkAccess getChunk(BlockPos pos) {
        return this.delegate.getChunk(pos);
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        return this.delegate.getChunk(chunkX, chunkZ);
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus requiredStatus) {
        return this.delegate.getChunk(chunkX, chunkZ, requiredStatus);
    }

    @Override
    @Nullable
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.delegate.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    public boolean isWaterAt(BlockPos pos) {
        return this.delegate.isWaterAt(pos);
    }

    @Override
    public boolean containsAnyLiquid(AABB bb) {
        return this.delegate.containsAnyLiquid(bb);
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos pos) {
        return this.delegate.getMaxLocalRawBrightness(pos);
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos pos, int amount) {
        return this.delegate.getMaxLocalRawBrightness(pos, amount);
    }

    @Override
    public boolean hasChunkAt(int x, int z) {
        return this.delegate.hasChunkAt(x, z);
    }

    @Override
    public boolean hasChunkAt(BlockPos pos) {
        return this.delegate.hasChunkAt(pos);
    }

    @Override
    public boolean hasChunksAt(BlockPos from, BlockPos to) {
        return this.delegate.hasChunksAt(from, to);
    }

    @Override
    public boolean hasChunksAt(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return this.delegate.hasChunksAt(fromX, fromY, fromZ, toX, toY, toZ);
    }

    @Override
    public boolean hasChunksAt(int fromX, int fromZ, int toX, int toZ) {
        return this.delegate.hasChunksAt(fromX, fromZ, toX, toZ);
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.delegate.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.delegate.enabledFeatures();
    }

    @Override
    public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> resourceKey) {
        return this.delegate.holderLookup(resourceKey);
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return this.delegate.environmentAttributes();
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.delegate.getLightEngine();
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos blockPos) {
        return this.delegate.getBrightness(lightType, blockPos);
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int amount) {
        return this.delegate.getRawBrightness(blockPos, amount);
    }

    @Override
    public boolean canSeeSky(BlockPos blockPos) {
        return this.delegate.canSeeSky(blockPos);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.delegate.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.delegate.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.delegate.getFluidState(pos);
    }

    @Override
    public int getLightEmission(BlockPos pos) {
        return this.delegate.getLightEmission(pos);
    }

    @Override
    public Stream<BlockState> getBlockStates(AABB area) {
        return this.delegate.getBlockStates(area);
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext context) {
        return this.delegate.isBlockInLine(context);
    }

    @Override
    public BlockHitResult clip(ClipContext context) {
        return this.delegate.clip(context);
    }

    @Override
    @Nullable
    public BlockHitResult clipWithInteractionOverride(Vec3 startVec, Vec3 endVec, BlockPos pos, VoxelShape shape, BlockState state) {
        return this.delegate.clipWithInteractionOverride(startVec, endVec, pos, shape, state);
    }

    @Override
    public double getBlockFloorHeight(VoxelShape shape, Supplier<VoxelShape> belowShapeSupplier) {
        return this.delegate.getBlockFloorHeight(shape, belowShapeSupplier);
    }

    @Override
    public double getBlockFloorHeight(BlockPos pos) {
        return this.delegate.getBlockFloorHeight(pos);
    }

    public static <T, C> T traverseBlocks(Vec3 from, Vec3 to, C context, BiFunction<C, BlockPos, T> tester, Function<C, T> onFail) {
        return BlockGetter.traverseBlocks(from, to, context, tester, onFail);
    }

    @Override
    public int getSectionsCount() {
        return this.delegate.getSectionsCount();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return this.delegate.isOutsideBuildHeight(pos);
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return this.delegate.isOutsideBuildHeight(y);
    }

    @Override
    public int getSectionIndex(int y) {
        return this.delegate.getSectionIndex(y);
    }

    @Override
    public int getSectionIndexFromSectionY(int sectionIndex) {
        return this.delegate.getSectionIndexFromSectionY(sectionIndex);
    }

    @Override
    public int getSectionYFromSectionIndex(int sectionIndex) {
        return this.delegate.getSectionYFromSectionIndex(sectionIndex);
    }

    public static LevelHeightAccessor create(int minBuildHeight, int height) {
        return LevelHeightAccessor.create(minBuildHeight, height);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.delegate.getWorldBorder();
    }

    @Override
    public boolean isUnobstructed(BlockState state, BlockPos pos, CollisionContext context) {
        return this.delegate.isUnobstructed(state, pos, context);
    }

    @Override
    public boolean isUnobstructed(Entity entity) {
        return this.delegate.isUnobstructed(entity);
    }

    @Override
    public boolean noCollision(AABB collisionBox) {
        return this.delegate.noCollision(collisionBox);
    }

    @Override
    public boolean noCollision(Entity entity) {
        return this.delegate.noCollision(entity);
    }

    @Override
    public boolean noCollision(@Nullable Entity entity, AABB collisionBox) {
        return this.delegate.noCollision(entity, collisionBox);
    }

    @Override
    public Iterable<VoxelShape> getCollisions(@Nullable Entity entity, AABB collisionBox) {
        return this.delegate.getCollisions(entity, collisionBox);
    }

    @Override
    public Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AABB collisionBox) {
        return this.delegate.getBlockCollisions(entity, collisionBox);
    }

    @Override
    public boolean collidesWithSuffocatingBlock(@Nullable Entity entity, AABB box) {
        return this.delegate.collidesWithSuffocatingBlock(entity, box);
    }

    @Override
    public Optional<Vec3> findFreePosition(@Nullable Entity entity, VoxelShape shape, Vec3 pos, double x, double y, double z) {
        return this.delegate.findFreePosition(entity, shape, pos, x, y, z);
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return this.delegate.isStateAtPosition(pos, state);
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return this.delegate.isFluidAtPosition(pos, predicate);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        return this.delegate.setBlock(pos, state, flags, recursionLeft);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
        return this.delegate.setBlock(pos, newState, flags);
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        return this.delegate.removeBlock(pos, isMoving);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        return this.delegate.destroyBlock(pos, dropBlock);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity) {
        return this.delegate.destroyBlock(pos, dropBlock, entity);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        return this.delegate.destroyBlock(pos, dropBlock, entity, recursionLeft);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return this.delegate.addFreshEntity(entity);
    }

    @Override
    public void gameEvent(ResourceKey<GameEvent> p_316780_, BlockPos p_316509_, GameEvent.Context p_316524_) {
        this.delegate.gameEvent(p_316780_, p_316509_, p_316524_);
    }

    @Override
    public boolean isAreaLoaded(BlockPos center, int range) {
        return this.delegate.isAreaLoaded(center, range);
    }

    @Override
    @ApiStatus.NonExtendable
    public @Nullable AuxiliaryLightManager getAuxLightManager(BlockPos pos) {
        return this.delegate.getAuxLightManager(pos);
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return this.delegate.getAuxLightManager(pos);
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        return this.delegate.getModelData(pos);
    }

    @Override
    public boolean noBlockCollision(@Nullable Entity pEntity, AABB pBoundingBox) {
        return this.delegate.noBlockCollision(pEntity, pBoundingBox);
    }

    @Override
    public Optional<BlockPos> findSupportingBlock(Entity pEntity, AABB pBox) {
        return this.delegate.findSupportingBlock(pEntity, pBox);
    }

    @Override
    public int getDirectSignalTo(BlockPos pPos) {
        return this.delegate.getDirectSignalTo(pPos);
    }

    @Override
    public int getControlInputSignal(BlockPos pPos, Direction pDirection, boolean pDiodesOnly) {
        return this.delegate.getControlInputSignal(pPos, pDirection, pDiodesOnly);
    }

    @Override
    public boolean hasSignal(BlockPos pPos, Direction pDirection) {
        return this.delegate.hasSignal(pPos, pDirection);
    }

    @Override
    public int getSignal(BlockPos pPos, Direction pDirection) {
        return this.delegate.getSignal(pPos, pDirection);
    }

    @Override
    public boolean hasNeighborSignal(BlockPos pPos) {
        return this.delegate.hasNeighborSignal(pPos);
    }

    @Override
    public int getBestNeighborSignal(BlockPos pPos) {
        return this.delegate.getBestNeighborSignal(pPos);
    }
}
