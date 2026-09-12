package dev.anvilcraft.lib.v2.registrum.builders.villager;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.function.Supplier;

public class PoiTypeBuilder<P> extends SelfBuilder<PoiType, P, PoiTypeBuilder<P>> {
    public PoiTypeBuilder(AbstractRegistrum<?> owner,
                          P parent,
                          String name,
                          BuilderCallback callback,
                          Supplier<Set<BlockState>> matchingStates,
                          int maxTickets,
                          int validRange) {
        super(owner, parent, name, callback, Registries.POINT_OF_INTEREST_TYPE, () -> new PoiType(matchingStates.get(), maxTickets, validRange));
    }
}
