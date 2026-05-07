package dev.anvilcraft.lib.v2.rendering.sdf;

public enum SdfRenderType {
    BOX,
    CIRCLE,
    ARC,
    SECTOR,
    PIE,
    CAPSULE,
    EGG;

    private static final SdfRenderType[] VALUES = values();

    public static SdfRenderType fromOrdinal(int ordinal) {
        return VALUES[ordinal];
    }
}
