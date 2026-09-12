package dev.anvilcraft.lib.v2.sync.init;

import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.management.SyncRegisterEntry;
import dev.anvilcraft.lib.v2.sync.util.SideUtil;
import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class AnvilLibSyncEntries {
    public static final DeferredRegister<SyncRegisterEntry<?, ?>> SYNC_ENTRY = DeferredRegister.create(
        AnvilLibSyncRegistries.SYNC_ENTRY,
        AnvilLibSync.MOD_ID
    );

    public static final DeferredHolder<SyncRegisterEntry<?, ?>, SyncRegisterEntry<Class<?>, String>> STATIC = SYNC_ENTRY.register(
        "static",
        () -> SyncRegisterEntry.create(
            Util.cast(Class.class),
            ByteBufCodecs.STRING_UTF8,
            Class::getName,
            (ignored, className) -> Class.forName(className),
            false,
            null
        )
    );

    public static final DeferredHolder<SyncRegisterEntry<?, ?>, SyncRegisterEntry<Entity, UUID>> ENTITY = SYNC_ENTRY.register(
        "entity",
        () -> SyncRegisterEntry.create(
            Entity.class,
            UUIDUtil.STREAM_CODEC,
            Entity::getUUID,
            SideUtil::entityFinder,
            Entity::level
        )
    );

    public static final DeferredHolder<SyncRegisterEntry<?, ?>, SyncRegisterEntry<BlockEntity, BlockPos>> BLOCK_ENTITY = SYNC_ENTRY.register(
        "block_entity",
        () -> SyncRegisterEntry.create(
            BlockEntity.class,
            BlockPos.STREAM_CODEC,
            BlockEntity::getBlockPos,
            SideUtil::blockEntityFinder,
            BlockEntity::getLevel
        )
    );
}
