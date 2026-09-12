package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.query;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query.GpuQueryObject;
import dev.anvilcraft.lib.v2.rendering.foundation.GpuReusableResourcePool;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

@ApiStatus.Internal
public class GpuSampleQueryPool extends GpuReusableResourcePool<GpuQueryObject, ALRGpuDeviceExtension> {
    public GpuSampleQueryPool(ALRGpuDeviceExtension context) {
        super(16, context);
    }

    @Override
    @NonNull
    protected GpuQueryObject createInstance(@NonNull ALRGpuDeviceExtension context, int i) {
        return context.alrCreateSamplesQuery();
    }
}
