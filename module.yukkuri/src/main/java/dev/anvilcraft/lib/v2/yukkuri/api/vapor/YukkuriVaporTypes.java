package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import dev.anvilcraft.lib.v2.yukkuri.Yukkuri;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Shared vapor identifiers. Addons may freely use additional resource locations. */
public final class YukkuriVaporTypes {
    public static final ResourceLocation GASEOUS_OIL = Yukkuri.of("gaseous_oil");
    public static final ResourceLocation GASEOUS_WATER = Yukkuri.of("gaseous_water");

    private YukkuriVaporTypes() {
    }

    public static boolean isStandard(@Nullable ResourceLocation id) {
        return GASEOUS_OIL.equals(id) || GASEOUS_WATER.equals(id);
    }
}
