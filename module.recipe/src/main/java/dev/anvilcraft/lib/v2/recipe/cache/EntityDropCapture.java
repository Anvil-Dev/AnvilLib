package dev.anvilcraft.lib.v2.recipe.cache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

/** 仅在一次实体消耗期间拦截物品入世界，避免容器移除掉落绕过配方提交。 */
public final class EntityDropCapture implements AutoCloseable {
    private static final ThreadLocal<EntityDropCapture> CURRENT = new ThreadLocal<>();
    private final EntityDropCapture previous;
    private final ServerLevel level;
    private final List<ItemEntity> drops = new ArrayList<>();

    public EntityDropCapture(ServerLevel level) {
        this.level = level;
        this.previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static boolean capture(Entity entity) {
        EntityDropCapture scope = CURRENT.get();
        if (scope == null || entity.level() != scope.level || !(entity instanceof ItemEntity item)) return false;
        scope.drops.add(item);
        return true;
    }

    public List<ItemEntity> drops() {
        return List.copyOf(this.drops);
    }

    @Override
    public void close() {
        if (this.previous == null) CURRENT.remove();
        else CURRENT.set(this.previous);
    }
}
