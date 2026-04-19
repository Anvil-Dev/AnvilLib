package dev.anvilcraft.lib.v2.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

import java.util.function.Supplier;

public abstract class DistExecutor {
    public static void run(Dist expectedDist, Supplier<Runnable> supplier) {
        if (FMLLoader.getDist() == expectedDist) {
            supplier.get().run();
        }
    }
}
