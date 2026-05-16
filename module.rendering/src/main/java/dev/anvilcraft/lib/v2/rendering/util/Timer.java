package dev.anvilcraft.lib.v2.rendering.util;

import net.minecraft.client.Minecraft;

public class Timer {
    public static float getPartialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(Minecraft.getInstance().isPaused());
    }
}
