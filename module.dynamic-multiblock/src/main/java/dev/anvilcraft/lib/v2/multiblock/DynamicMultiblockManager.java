package dev.anvilcraft.lib.v2.multiblock;

import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class DynamicMultiblockManager {

    // ========================= ↓ 静态方法 ↓ ========================= //

    private static final Map<Level, DynamicMultiblockManager> MANAGERS = new HashMap<>();

    public static DynamicMultiblockManager get(Level level) {
        return DynamicMultiblockManager.MANAGERS.computeIfAbsent(level, DynamicMultiblockManager::new);
    }

    // ========================= ↓ 成员方法 ↓ ========================= //

    private transient final Level level;

    public DynamicMultiblockManager(Level level) {
        this.level = level;
    }
}
