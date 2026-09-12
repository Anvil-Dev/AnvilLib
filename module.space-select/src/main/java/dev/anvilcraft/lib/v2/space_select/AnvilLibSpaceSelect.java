package dev.anvilcraft.lib.v2.space_select;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@Mod(AnvilLibSpaceSelect.MOD_ID)
public class AnvilLibSpaceSelect {
    @ApiStatus.Internal
    public AnvilLibSpaceSelect() {
    }

    public static final String MOD_ID = "anvillib_space_select";

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnvilLibSpaceSelect.MOD_ID, path);
    }
}
