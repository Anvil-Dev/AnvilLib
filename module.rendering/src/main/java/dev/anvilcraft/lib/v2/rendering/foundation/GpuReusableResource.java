package dev.anvilcraft.lib.v2.rendering.foundation;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface GpuReusableResource extends AutoCloseable {

    void acquire();

    void release();

    boolean isAcquired();

    @Override
    void close();
}
