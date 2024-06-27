package dev.anvilcraft.lib;


import dev.anvilcraft.lib.util.Platform;
import dev.architectury.injectables.annotations.ExpectPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnvilLib {
    public static final String MOD_ID = "anvillib";
    public static final String MOD_NAME = "AnvilLib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {

    }

    @ExpectPlatform
    public static Platform getPlatform() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isLoaded(String modid) {
        throw new AssertionError();
    }
}
