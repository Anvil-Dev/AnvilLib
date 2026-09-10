package dev.anvilcraft.lib.v2.rendering.foundation.compound;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface DirtyTracked {
    void markDirty();
}
