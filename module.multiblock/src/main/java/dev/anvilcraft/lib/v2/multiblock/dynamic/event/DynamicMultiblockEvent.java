package dev.anvilcraft.lib.v2.multiblock.dynamic.event;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import lombok.Getter;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/// 所有与动态多方块结构有关的事件的基类。
@Getter
public abstract class DynamicMultiblockEvent extends Event {
    private final Level level;
    private final IController controller;
    private final MultiblockState state;

    protected DynamicMultiblockEvent(Level level, IController controller, MultiblockState state) {
        this.level = level;
        this.controller = controller;
        this.state = state;
    }

    /// 本事件会在多方块结构尝试形成时发出。
    ///
    /// 取消该事件将阻止多方块结构的形成。
    ///
    /// 本事件会在双端发出。
    public static class Form extends DynamicMultiblockEvent implements ICancellableEvent {
        public Form(Level level, IController controller, MultiblockState state) {
            super(level, controller, state);
        }
    }

    /// 本事件会在多方块结构尝试解散时发出。
    ///
    /// 取消该事件将阻止多方块结构的解散。
    ///
    /// 本事件会在双端发出。
    public static class Unform extends DynamicMultiblockEvent implements ICancellableEvent {
        public Unform(Level level, IController controller, MultiblockState state) {
            super(level, controller, state);
        }
    }
}
