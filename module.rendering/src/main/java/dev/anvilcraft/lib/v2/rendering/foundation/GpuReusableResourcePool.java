package dev.anvilcraft.lib.v2.rendering.foundation;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class GpuReusableResourcePool<T extends GpuReusableResource, C> extends LoopResetPool<T, C> {

    public GpuReusableResourcePool(int size, C context) {
        super(size, context);
    }

    @Override
    public void release(T query) {
        query.release();
    }

    @Override
    public void destroy(T query) {
        query.close();
    }

    @Override
    public void onAcquire(T query) {
        query.acquire();
    }

    @Override
    public boolean isAvailable(T query) {
        return !query.isAcquired();
    }

    @Override
    public T fail(boolean createInstanceIfAllAcquired) {
        int index = 0;

        if (createInstanceIfAllAcquired) {
            index = this.size;
            expand();
        }

        T query = this.get(index);
        query.acquire();

        return query;
    }
}
