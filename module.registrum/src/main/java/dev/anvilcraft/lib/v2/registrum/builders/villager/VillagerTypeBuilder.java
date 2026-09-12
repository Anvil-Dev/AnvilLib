package dev.anvilcraft.lib.v2.registrum.builders.villager;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.VillagerType;

public class VillagerTypeBuilder<P> extends SelfBuilder<VillagerType, P, VillagerTypeBuilder<P>> {

    public VillagerTypeBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        // 1.21.1 的 VillagerType 仅有 String 构造，不能直接方法引用为 Supplier，须用 lambda
        super(owner, parent, name, callback, Registries.VILLAGER_TYPE, () -> new VillagerType(name));
    }
}
