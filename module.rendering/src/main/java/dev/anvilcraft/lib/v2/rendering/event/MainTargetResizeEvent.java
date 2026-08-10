package dev.anvilcraft.lib.v2.rendering.event;

import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

@Getter
public class MainTargetResizeEvent extends Event implements IModBusEvent {
    private final int newWidth;
    private final int newHeight;

    public MainTargetResizeEvent(int newWidth, int newHeight) {
        this.newWidth = newWidth;
        this.newHeight = newHeight;
    }
}
