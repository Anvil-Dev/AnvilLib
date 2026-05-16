package dev.anvilcraft.lib.v2.registrum.builders.villager;

import com.google.common.collect.ImmutableSet;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public class VillagerProfessionBuilder<P> extends SelfBuilder<VillagerProfession, P, VillagerProfessionBuilder<P>> {
    public VillagerProfessionBuilder(AbstractRegistrum<?> owner,
                                     P parent,
                                     String name,
                                     BuilderCallback callback,
                                     Predicate<Holder<PoiType>> heldJobSite,
                                     Predicate<Holder<PoiType>> acquirableJobSite,
                                     ImmutableSet<Item> requestedItems,
                                     ImmutableSet<Block> secondaryPoi,
                                     @Nullable SoundEvent workSound,
                                     Int2ObjectMap<ResourceKey<TradeSet>> tradeSetsByLevel) {
        super(owner, parent, name, callback,
                Registries.VILLAGER_PROFESSION, () -> new VillagerProfession(
                Component.translatable("entity." + owner.getModid() + ".villager." + name),
                heldJobSite,
                acquirableJobSite,
                requestedItems,
                secondaryPoi,
                workSound,
                tradeSetsByLevel
        ));
    }

    public VillagerProfessionBuilder(AbstractRegistrum<?> owner,
                                     P parent,
                                     String name,
                                     BuilderCallback callback,
                                     Predicate<Holder<PoiType>> heldJobSite,
                                     Predicate<Holder<PoiType>> acquirableJobSite,
                                     @Nullable SoundEvent workSound,
                                     Int2ObjectMap<ResourceKey<TradeSet>> tradeSetsByLevel) {
        this(owner, parent, name, callback, heldJobSite, acquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), workSound, tradeSetsByLevel);
    }



    public VillagerProfessionBuilder(AbstractRegistrum<?> owner,
                                     P parent,
                                     String name,
                                     BuilderCallback callback,
                                     ResourceKey<PoiType> jobSite,
                                     @Nullable SoundEvent workSound,
                                     Int2ObjectMap<ResourceKey<TradeSet>> tradeSetsByLevel
                                     ) {
        this(owner, parent, name, callback, h -> h.is(jobSite), h -> h.is(jobSite), workSound, tradeSetsByLevel);
    }
}
