package dev.anvilcraft.lib.v2.registrum.builders.villager;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.villager.VillagerType;

public class VillagerTypeBuilder<P> extends SelfBuilder<VillagerType, P, VillagerTypeBuilder<P>> {

    public VillagerTypeBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.VILLAGER_TYPE, VillagerType::new);
    }
}
