package dev.anvilcraft.lib.v2.rpc;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(AnvilLibRpc.MOD_ID)
public class AnvilLibRpc {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_rpc";

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibRpc.MAIN_ID, path);
    }
}