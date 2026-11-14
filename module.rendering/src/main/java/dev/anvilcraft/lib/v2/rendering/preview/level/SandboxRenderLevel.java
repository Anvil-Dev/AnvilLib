package dev.anvilcraft.lib.v2.rendering.preview.level;

import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 用于结构预览的隔离式客户端轻量关卡。
 *
 * <p>只实现渲染与结构放置所需能力，其他与玩法相关系统均采用桩实现。</p>
 */
public class SandboxRenderLevel extends Level {
    private static final ResourceKey<Level> LEVEL_ID = ResourceKey.create(
        Registries.DIMENSION,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AnvilLibRendering.MODID, "structure")
    );

    private final TransientEntitySectionManager<Entity> entitySectionManager = new TransientEntitySectionManager<>(
        Entity.class, new LevelCallbackCallbacks()
    );
    private final ChunkSource chunkSource = new SandboxChunkProvider(this);
    private final Holder<Biome> biome;
    private final RegistryAccess registryAccess;
    private final FeatureFlagSet enabledFeatures;
    private final LongSet filledBlocks = new LongOpenHashSet();
    private final Map<BlockPos, CompoundTag> structureBlockEntityData = new HashMap<>();
    private final Map<BlockPos, BlockEntity> inspectionBlockEntities = new HashMap<>();

    private final LongSet litSections = new LongOpenHashSet();
    private final DataLayer defaultDataLayer;

    private final TickRateManager tickRateManager = new TickRateManager();
    private final ClientLevel.ClientLevelData clientLevelData;
    private final DeltaTracker.Timer tracker = new DeltaTracker.Timer(20.0F, 0L, def -> def);
    @Getter
    private float partialTick;

    /** 保存放置前的结构数据，防止加载回调改写配置。 */
    void captureBlockEntityData(CompoundTag template, BlockPos placementPos) {
        this.structureBlockEntityData.clear();
        this.structureBlockEntityData.putAll(StructureBlockEntityDetails.capture(template, placementPos));
    }

    public @Nullable CompoundTag getStructureBlockEntityData(BlockPos pos) {
        CompoundTag data = this.structureBlockEntityData.get(pos);
        return data == null ? null : data.copy();
    }

    /** 重新加载保存状态，避免原生加载回调的红石和计时更新影响只读菜单。 */
    public void restoreBlockEntitiesForInspection() {
        this.structureBlockEntityData.forEach((pos, data) -> {
            BlockEntity entity = BlockEntity.loadStatic(pos, this.getBlockState(pos), data.copy(), this.registryAccess());
            if (entity != null) {
                entity.setLevel(this);
                this.inspectionBlockEntities.put(pos, entity);
            }
        });
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        BlockEntity entity = this.inspectionBlockEntities.get(pos);
        return entity != null ? entity : super.getBlockEntity(pos);
    }

    public SandboxRenderLevel() {
        this(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess());
    }

    public SandboxRenderLevel(RegistryAccess registryAccess) {
        this(registryAccess, FeatureFlags.DEFAULT_FLAGS);
    }

    /** 使用当前世界启用的特性创建沙盒，保证实体类型与预览一致。 */
    public SandboxRenderLevel(RegistryAccess registryAccess, FeatureFlagSet enabledFeatures) {
        this(createLevelData(), registryAccess, enabledFeatures);
    }

    private SandboxRenderLevel(ClientLevel.ClientLevelData levelData, RegistryAccess registryAccess, FeatureFlagSet enabledFeatures) {
        super(
            levelData,
            LEVEL_ID,
            registryAccess,
            registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE)
                .getOrThrow(BuiltinDimensionTypes.OVERWORLD),
            true,
            false,
            0,
            1000000
        );
        this.clientLevelData = levelData;
        this.registryAccess = registryAccess;
        this.enabledFeatures = enabledFeatures;
        this.biome = registryAccess.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);

        var nibbles = new byte[DataLayer.SIZE];
        Arrays.fill(nibbles, (byte) 0xFF);
        this.defaultDataLayer = new DataLayer(nibbles);
    }

    private static ClientLevel.ClientLevelData createLevelData() {
        var levelData = new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, false);
        levelData.setDayTime(6000);
        return levelData;
    }

    @Override
    public void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState1, int i) {
    }

    @Override
    public void playSeededSound(
        @Nullable Entity player,
        double v,
        double v1,
        double v2,
        Holder<SoundEvent> holder,
        SoundSource soundSource,
        float v3,
        float v4,
        long l
    ) {
    }

    @Override
    public void playSeededSound(
        @Nullable Entity player,
        Entity entity,
        Holder<SoundEvent> holder,
        SoundSource soundSource,
        float v,
        float v1,
        long l
    ) {
    }

    @Override
    public String gatherChunkSourceStats() {
        return "";
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return this.entitySectionManager.getEntityGetter();
    }

    @Override
    public @Nullable Entity getEntity(int id) {
        return this.getEntities().get(id);
    }

    /**
     * 返回当前预览关卡中可供渲染遍历的实体集合。
     */
    public Iterable<Entity> getEntitiesForRendering() {
        return this.entitySectionManager.getEntityGetter().getAll();
    }

    public void addEntity(Entity entity) {
        this.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        this.entitySectionManager.addEntity(entity);
        entity.onAddedToLevel();
        this.refreshLightingAround(entity.getOnPos());
    }

    public void removeEntity(int entityId, Entity.RemovalReason reason) {
        Entity entity = getEntities().get(entityId);
        if (entity != null) {
            entity.setRemoved(reason);
            entity.onClientRemoval();
        }
    }

    public record Bounds(BlockPos min, BlockPos max) {
    }

    /**
     * 推进游戏时间并计算用于渲染插值的 partialTick。
     */
    public void tickFrameClock() {
        var ticksElapsed = tracker.advanceTime(Util.getMillis(), true);
        if (ticksElapsed > 0) {
            clientLevelData.setGameTime(clientLevelData.getGameTime() + ticksElapsed);
        }

        partialTick = tracker.getGameTimeDeltaPartialTick(false);
    }

    /** 判断预览是否包含方块或实体，供仅有实体的结构选择投影和边界。 */
    public boolean hasContent() {
        return this.hasFilledBlocks() || this.getEntitiesForRendering().iterator().hasNext();
    }

    public boolean hasFilledBlocks() {
        return !this.filledBlocks.isEmpty();
    }

    /**
     * 返回当前已加载预览区块中所有非空气方块位置。
     */
    public Stream<BlockPos> getFilledBlocks() {
        var mutablePos = new BlockPos.MutableBlockPos();
        return this.filledBlocks.longStream()
            .sequential()
            .mapToObj(pos -> {
                mutablePos.set(pos);
                return mutablePos;
            });
    }

    /**
     * 计算包围所有已渲染方块与实体的世界坐标轴对齐包围盒。
     */
    public Bounds getBounds() {
        if (!this.hasContent()) {
            return new Bounds(BlockPos.ZERO, BlockPos.ZERO);
        }

        var min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        var max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        var cur = new BlockPos.MutableBlockPos();

        this.filledBlocks.forEach(packedPos -> {
            cur.set(packedPos);
            min.setX(Math.min(min.getX(), cur.getX()));
            min.setY(Math.min(min.getY(), cur.getY()));
            min.setZ(Math.min(min.getZ(), cur.getZ()));

            max.setX(Math.max(max.getX(), cur.getX() + 1));
            max.setY(Math.max(max.getY(), cur.getY() + 1));
            max.setZ(Math.max(max.getZ(), cur.getZ() + 1));
        });

        for (var entity : getEntitiesForRendering()) {
            var bounds = entity.getBoundingBox();

            min.setX(Math.min(min.getX(), Mth.floor(bounds.minX)));
            min.setY(Math.min(min.getY(), Mth.floor(bounds.minY)));
            min.setZ(Math.min(min.getZ(), Mth.floor(bounds.minZ)));

            max.setX(Math.max(max.getX(), (int) Math.ceil(bounds.maxX)));
            max.setY(Math.max(max.getY(), (int) Math.ceil(bounds.maxY)));
            max.setZ(Math.max(max.getZ(), (int) Math.ceil(bounds.maxZ)));
        }

        return new Bounds(min, max);
    }

    public boolean isFilledBlock(BlockPos blockPos) {
        return this.filledBlocks.contains(blockPos.asLong());
    }

    void removeFilledBlock(BlockPos pos) {
        this.filledBlocks.remove(pos.asLong());
    }

    void addFilledBlock(BlockPos pos) {
        this.filledBlocks.add(pos.asLong());
    }

    /**
     * 预热指定位置周围的光照数据，避免预览渲染出现黑块区域。
     */
    public void refreshLightingAround(BlockPos pos) {
        var minChunk = new ChunkPos(pos.offset(-1, -1, -1));
        var maxChunk = new ChunkPos(pos.offset(1, 1, 1));
        ChunkPos.rangeClosed(minChunk, maxChunk).forEach(chunkPos -> {
            if (this.litSections.add(chunkPos.toLong())) {
                var lightEngine = getLightEngine();
                for (int i = 0; i < getSectionsCount(); ++i) {
                    int y = getSectionYFromSectionIndex(i);
                    var sectionPos = SectionPos.of(chunkPos, y);
                    lightEngine.updateSectionStatus(sectionPos, false);
                    lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, this.defaultDataLayer);
                    lightEngine.queueSectionData(LightLayer.SKY, sectionPos, this.defaultDataLayer);
                }
                lightEngine.setLightEnabled(chunkPos, true);
                lightEngine.propagateLightSources(chunkPos);
                lightEngine.retainData(chunkPos, false);
            }
        });
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.tickRateManager;
    }

    @Override
    public @Nullable MapItemSavedData getMapData(MapId mapId) {
        return null;
    }



    @Override
    public void destroyBlockProgress(int i, BlockPos blockPos, int i1) {
    }

    @Override
    public Scoreboard getScoreboard() {
        return new Scoreboard();
    }

    @Override
    public PotionBrewing potionBrewing() {
        throw new UnsupportedOperationException();
    }

    private float dayTimeFraction;
    private float dayTimePerTick = 1;

    @Override
    public void setDayTimeFraction(float fraction) { this.dayTimeFraction = fraction; }
    @Override
    public float getDayTimeFraction() { return this.dayTimeFraction; }
    @Override
    public void setDayTimePerTick(float speed) { this.dayTimePerTick = speed; }
    @Override
    public float getDayTimePerTick() { return this.dayTimePerTick; }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.chunkSource;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    @Override
    public void levelEvent(@Nullable Entity player, int i, BlockPos blockPos, int i1) {
    }

    @Override
    public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context) {
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) {
            return 1.0F;
        } else {
            return switch (direction) {
                case DOWN -> 0.5F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
                default -> 1.0F;
            };
        }
    }

    @Override
    public List<? extends Player> players() {
        return List.of();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int i, int i1, int i2) {
        return this.biome;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.enabledFeatures;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeAccess recipeAccess() {
        return java.util.Objects.requireNonNull(Minecraft.getInstance().level).recipeAccess();
    }

    @Override
    public net.minecraft.world.level.block.entity.FuelValues fuelValues() {
        return java.util.Objects.requireNonNull(Minecraft.getInstance().level).fuelValues();
    }





    @Override
    public java.util.Collection<net.neoforged.neoforge.entity.PartEntity<?>> dragonParts() {
        return java.util.List.of();
    }

    @Override
    public void explode(
        @Nullable Entity entity,
        net.minecraft.world.damagesource.@Nullable DamageSource damageSource,
        net.minecraft.world.level.@Nullable ExplosionDamageCalculator explosionDamageCalculator,
        double v,
        double v1,
        double v2,
        float v3,
        boolean b,
        ExplosionInteraction explosionInteraction,
        net.minecraft.core.particles.ParticleOptions particleOptions,
        net.minecraft.core.particles.ParticleOptions particleOptions1,
        Holder<SoundEvent> holder
    ) {

    }

    @Override
    public int getSeaLevel() { return 63; }

    private static class LevelCallbackCallbacks implements LevelCallback<Entity> {
        @Override
        public void onCreated(Entity entity) {
        }

        @Override
        public void onDestroyed(Entity entity) {
        }

        @Override
        public void onTickingStart(Entity entity) {
        }

        @Override
        public void onTickingEnd(Entity entity) {
        }

        @Override
        public void onTrackingStart(Entity entity) {
        }

        @Override
        public void onTrackingEnd(Entity entity) {
        }

        @Override
        public void onSectionChange(Entity object) {
        }
    }
}
