package dev.anvilcraft.lib.v2.rendering;

import com.mojang.blaze3d.vertex.PoseStack;

public class ALRSharedMath {
    public static final PoseStack.Pose IDENTITY_POSE_3D = new PoseStack.Pose();
    public static final float SQRT_2 = 1.4142135623730950488016887242097f;

    static {
        IDENTITY_POSE_3D.setIdentity();
    }
}
