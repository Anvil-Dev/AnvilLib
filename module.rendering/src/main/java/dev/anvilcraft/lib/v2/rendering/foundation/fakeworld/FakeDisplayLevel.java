package dev.anvilcraft.lib.v2.rendering.foundation.fakeworld;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FakeDisplayLevel extends Level implements BlockAndTintGetter {
    private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager<>(Entity.class, new EntityCallbacks());

    private final ClientLevel delegate;
    private final Map<BlockPos, BlockState> blockStates = new HashMap<>();
    private final Map<BlockPos, FluidState> fluidStates = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    public FakeDisplayLevel(ClientLevel delegate) {
        super(
            delegate.getLevelData(),
            delegate.dimension(),
            delegate.registryAccess(),
            delegate.dimensionTypeRegistration(),
            true,
            false,
            42,
            0
        );
        this.delegate = delegate;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        FluidState fluidState = this.fluidStates.get(pos);
        if (fluidState != null) return fluidState;
        return Fluids.EMPTY.defaultFluidState();
    }

    public void setFluidState(BlockPos pos, FluidState fluidState) {
        this.fluidStates.put(pos, fluidState);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState blockState = this.blockStates.get(pos);
        if (blockState != null) return blockState;

        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return this.blockEntities.get(pos);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags, int updateLimit) {
        this.blockStates.put(pos, blockState);
        return true;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        this.blockEntities.put(blockEntity.getBlockPos(), blockEntity);
    }

    public void setBlockEntity(BlockPos pos, @Nullable BlockEntity entity) {
        this.blockEntities.put(pos, entity);
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState current, @Block.UpdateFlags int updateFlags) {
    }

    @Override
    public void playSeededSound(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
    }

    @Override
    public void playSeededSound(@Nullable Entity except, Entity sourceEntity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
    }

    @Override
    public void explode(@Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float r, boolean fire, ExplosionInteraction interactionType, ParticleOptions smallExplosionParticles, ParticleOptions largeExplosionParticles, WeightedList<ExplosionParticleInfo> blockParticles, Holder<SoundEvent> explosionSound) {
    }

    @Override
    public String gatherChunkSourceStats() {
        return this.delegate.gatherChunkSourceStats();
    }

    @Override
    public void setRespawnData(LevelData.RespawnData respawnData) {
        this.delegate.setRespawnData(respawnData);
    }

    @Override
    public LevelData.RespawnData getRespawnData() {
        return this.delegate.getRespawnData();
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        this.entityStorage.addEntity(entity);
        return true;
    }

    @Override
    public @Nullable Entity getEntity(int id) {
        return this.getEntities().get(id);
    }

    @Override
    public Collection<? extends PartEntity<?>> dragonParts() {
        return this.delegate.dragonParts();
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.delegate.tickRateManager();
    }

    @Override
    public @Nullable MapItemSavedData getMapData(MapId id) {
        return this.delegate.getMapData(id);
    }

    @Override
    public void destroyBlockProgress(int id, BlockPos blockPos, int progress) {
        this.delegate.destroyBlockProgress(id, blockPos, progress);
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.delegate.getScoreboard();
    }

    @Override
    public RecipeAccess recipeAccess() {
        return this.delegate.recipeAccess();
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return this.entityStorage.getEntityGetter();
    }

    @Override
    public ClockManager clockManager() {
        return this.delegate.clockManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return this.delegate.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.delegate.enabledFeatures();
    }

    @Override
    public EnvironmentAttributeSystem environmentAttributes() {
        return this.delegate.environmentAttributes();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return this.delegate.potionBrewing();
    }

    @Override
    public FuelValues fuelValues() {
        return this.delegate.fuelValues();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.delegate.getChunkSource();
    }

    @Override
    public void levelEvent(@Nullable Entity source, int type, BlockPos pos, int data) {
        this.delegate.levelEvent(source, type, pos, data);
    }

    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context) {
        this.delegate.gameEvent(gameEvent, position, context);
    }

    @Override
    public List<? extends Player> players() {
        return this.delegate.players();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.delegate.getWorldBorder();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.delegate.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.delegate.getFluidTicks();
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        Optional<Holder.Reference<Biome>> biomeReference = registryAccess.get(Biomes.PLAINS);
        return biomeReference.map(reference -> color.getColor(reference.value(), pos.getX(), pos.getZ())).orElse(-1);
    }

    private static class EntityCallbacks implements LevelCallback<Entity> {

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
        public void onSectionChange(Entity entity) {
        }
    }
}
