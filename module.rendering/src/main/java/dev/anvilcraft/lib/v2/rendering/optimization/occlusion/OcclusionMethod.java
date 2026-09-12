package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRHICapabilities;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.ALRComputeCapabilities;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.HierarchicalZOcclusionCuller;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.noop.NoOpOcclusionCuller;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.query.GpuQueryOcclusionCuller;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public enum OcclusionMethod {
    GPU_QUERY {
        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public @NonNull OcclusionCuller createInstance(GpuDevice device) {
            return new GpuQueryOcclusionCuller((ALRGpuDeviceExtension) device);
        }
    }, HIERARCHICAL_Z {
        @Override
        public boolean isSupported() {
            boolean textureEnoughToUse = ALRHICapabilities.getInstance().bindlessTexturing() || ALRHICapabilities.getInstance().maxImageUnit() > 16;
            return ALRComputeCapabilities.isComputeSupported() && textureEnoughToUse;
        }

        @Override
        public @NonNull OcclusionCuller createInstance(GpuDevice device) {
            return new HierarchicalZOcclusionCuller((ALRGpuDeviceExtension) device);
        }
    }, NO_OP {
        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public @NonNull OcclusionCuller createInstance(GpuDevice device) {
            return new NoOpOcclusionCuller();
        }
    };

    public abstract boolean isSupported();

    @Nullable
    public abstract OcclusionCuller createInstance(GpuDevice device);
}
