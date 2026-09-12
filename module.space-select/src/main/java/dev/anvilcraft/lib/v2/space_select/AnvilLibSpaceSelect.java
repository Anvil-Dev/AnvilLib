package dev.anvilcraft.lib.v2.space_select;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@Mod(AnvilLibSpaceSelect.MOD_ID)
public class AnvilLibSpaceSelect {
    @ApiStatus.Internal
    public AnvilLibSpaceSelect() {
    }

    public static final String MOD_ID = "anvillib_space_select";

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibSpaceSelect.MOD_ID, path);
    }
}
