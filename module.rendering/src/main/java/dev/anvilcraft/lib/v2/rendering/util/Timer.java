package dev.anvilcraft.lib.v2.rendering.util;

import net.minecraft.client.Minecraft;

public class Timer {
    public static final int ONE_DAY_TICKS = 86400 * 20;

    public static float getPartialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(Minecraft.getInstance().isPaused()) % ONE_DAY_TICKS ;
    }
}
