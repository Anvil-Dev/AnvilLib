package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import org.jetbrains.annotations.ApiStatus;

/// Device policy for the hierarchical-Z backend.
///
/// The {@link dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.spd.SinglePassDownsampler SPD pass}
/// and the Hi-Z occlusion test bind their mip chain either as consecutive image
/// units, or - on devices that cannot spare that many units - through a bindless
/// image array:
///
/// - The image unit form needs at least 15 units: the SPD pass binds the mid mip
///   at unit 1 and thirteen mip views at units 2..14, and GLSL rejects a
///   `layout(binding = N)` index that is not below `GL_MAX_IMAGE_UNITS`.
///   {@link #REQUIRED_IMAGE_UNITS} is the smallest power of two that satisfies
///   this requirement.
/// - The bindless fallback is what makes hierarchical-Z usable on devices with
///   fewer image units, but it is broken on Intel Arc graphics under Windows:
///   the driver advertises `GL_ARB_bindless_texture`, yet using it crashes the JVM.
@ApiStatus.Internal
public final class HierarchicalZSupport {
    /// `GL_MAX_IMAGE_UNITS` needed to bind the SPD mip chain as image units.
    public static final int REQUIRED_IMAGE_UNITS = 16;

    private HierarchicalZSupport() {
    }

    /// Returns whether the device reports enough image units to bind the mip
    /// chain directly.
    ///
    /// @param device device to inspect
    /// @return `true` when the image unit form can be used
    public static boolean hasEnoughImageUnits(ALRGpuDeviceExtension device) {
        return device.alrhiCreateCapabilities().maxImageUnit() >= REQUIRED_IMAGE_UNITS;
    }

    /// Returns whether the bindless texturing path has to be used on the device.
    ///
    /// Bindless texturing is only the fallback for devices that cannot bind the
    /// mip chain as image units, so this is `false` as soon as
    /// {@link #hasEnoughImageUnits(ALRGpuDeviceExtension)} holds, even on devices
    /// that support it.
    ///
    /// @param device device to inspect
    /// @return `true` when the bindless path has to be and may be used
    public static boolean useBindlessTexturing(ALRGpuDeviceExtension device) {
        return !hasEnoughImageUnits(device)
            && device.alrhiCreateCapabilities().bindlessTexturing()
            && !device.alrhiCreateHeuristics().isWindowsArcGraphics();
    }

    /// Returns whether hierarchical-Z can run on the device at all.
    ///
    /// @param device device to inspect
    /// @return `true` when either image unit form or the bindless fallback works
    public static boolean available(ALRGpuDeviceExtension device) {
        return hasEnoughImageUnits(device) || useBindlessTexturing(device);
    }
}
