package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import dev.anvilcraft.lib.v2.yukkuri.Yukkuri;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public final class YukkuriCapabilities {
    public static final BlockCapability<IVaporConsumer, @Nullable Direction> VAPOR_CONSUMER =
        BlockCapability.createSided(Yukkuri.of("vapor_consumer"), IVaporConsumer.class);

    private YukkuriCapabilities() {
    }
}
