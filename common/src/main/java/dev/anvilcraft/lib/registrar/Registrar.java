package dev.anvilcraft.lib.registrar;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class Registrar extends AbstractRegistrar {
    private Registrar(String modid) {
        super(modid);
    }

    public static @NotNull Registrar create(String modid) {
        return new Registrar(modid);
    }
}
