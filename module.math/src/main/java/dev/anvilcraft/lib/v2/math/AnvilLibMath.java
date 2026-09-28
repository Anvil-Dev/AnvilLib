package dev.anvilcraft.lib.v2.math;

import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibMath.MOD_ID)
public class AnvilLibMath {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_math";

    public AnvilLibMath(IEventBus modEventBus, ModContainer ignored) {
        LibFunctionTypes.register(modEventBus);
        LibBuiltInFunctions.register(modEventBus);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnvilLibMath.MAIN_ID, path);
    }
}
