package dev.anvilcraft.lib.v2.rendering.preview.level;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 将 {@link StructureTemplate} 放入客户端沙盒关卡的辅助工厂。
 */
@Slf4j
public final class StructureSandboxFactory {
    private StructureSandboxFactory() {
    }

    /**
     * 使用给定的模板放置原点创建一个新的沙盒预览关卡。
     */
    public static @Nullable SandboxRenderLevel create(@Nullable Level clientLevel, StructureTemplate template, BlockPos placementPos) {
        if (clientLevel == null) {
            return null;
        }

        SingleThreadedRandomSource random = new SingleThreadedRandomSource(0L);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setIgnoreEntities(true);
        // 保留蓝图中的连接状态，避免邻接更新改变预览结构。
        settings.setKnownShape(true);

        SandboxRenderLevel level = new SandboxRenderLevel(clientLevel.registryAccess(), clientLevel.enabledFeatures());
        CompoundTag structureData = template.save(new CompoundTag());
        level.captureBlockEntityData(structureData, placementPos);
        DelegatingServerLevelAccessor fakeServerLevel = new DelegatingServerLevelAccessor(level);
        boolean placed = template.placeInWorld(fakeServerLevel, placementPos, placementPos, settings, random, Block.UPDATE_KNOWN_SHAPE);
        // 原版结构实体加载依赖服务端关卡，预览在客户端沙盒中重建实体。
        for (CompoundTag entry : structureData.getListOrEmpty("entities").compoundStream().toList()) {
            CompoundTag entityData = entry.getCompound("nbt").orElseThrow().copy();
            ListTag relativePos = entry.getListOrEmpty("pos");
            ListTag originalPos = entityData.getListOrEmpty("Pos");
            Vec3 offset = new Vec3(
                relativePos.getDoubleOr(0, 0) + placementPos.getX() - originalPos.getDoubleOr(0, 0),
                relativePos.getDoubleOr(1, 0) + placementPos.getY() - originalPos.getDoubleOr(1, 0),
                relativePos.getDoubleOr(2, 0) + placementPos.getZ() - originalPos.getDoubleOr(2, 0)
            );
            relocateEntityData(entityData, offset);
            Entity entity = EntityType.loadEntityRecursive(entityData, level, EntitySpawnReason.STRUCTURE, loaded -> {
                if (loaded instanceof FallingBlockEntity falling) {
                    falling.setStartPos(falling.blockPosition());
                }
                return loaded;
            });
            if (entity != null) {
                entity.getSelfAndPassengers().forEach(level::addEntity);
            }
        }
        if (!placed && !level.hasContent()) {
            log.debug("Failed to place structure preview into sandbox at {}", placementPos);
        }
        if (level.getEntitiesForRendering().iterator().hasNext()) {
            level.getLightEngine().runLightUpdates();
        }
        return level;
    }

    private static void relocateEntityData(CompoundTag data, Vec3 offset) {
        ListTag original = data.getListOrEmpty("Pos");
        ListTag position = new ListTag();
        position.add(DoubleTag.valueOf(original.getDoubleOr(0, 0) + offset.x));
        position.add(DoubleTag.valueOf(original.getDoubleOr(1, 0) + offset.y));
        position.add(DoubleTag.valueOf(original.getDoubleOr(2, 0) + offset.z));
        data.put("Pos", position);
        data.remove("UUID");
        data.getIntArray("block_pos").filter(pos -> pos.length == 3).ifPresent(pos -> data.putIntArray("block_pos", new int[]{
            pos[0] + (int) Math.round(offset.x), pos[1] + (int) Math.round(offset.y), pos[2] + (int) Math.round(offset.z)
        }));
        for (CompoundTag passenger : data.getListOrEmpty("Passengers").compoundStream().toList()) {
            relocateEntityData(passenger, offset);
        }
    }

    /**
     * 返回适合 GUI 预览的居中放置原点。
     */
    public static BlockPos centeredPlacement(StructureTemplate template) {
        Vec3i size = template.getSize();
        int x = (int) Math.ceil(size.getX() / 2.0d);
        int z = (int) Math.ceil(size.getZ() / 2.0d);
        return new BlockPos(-x, 0, -z);
    }
}
