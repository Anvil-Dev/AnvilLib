package dev.anvilcraft.lib.v2.util;

import lombok.experimental.UtilityClass;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

import java.util.function.Supplier;

@UtilityClass
public final class DistExecutor {
    public static void run(Dist expectedDist, Supplier<Runnable> supplier) {
        if (FMLLoader.getCurrent().getDist() == expectedDist) {
            supplier.get().run();
        }
    }
}
