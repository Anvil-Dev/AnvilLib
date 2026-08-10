package dev.anvilcraft.lib.v2.registrum.builders.villager;

import com.google.common.collect.ImmutableSet;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 注册村民职业。<p>
 * 注：1.21.1 的 {@link VillagerProfession} 是 6 元 record
 * {@code (String name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite,
 * ImmutableSet<Item> requestedItems, ImmutableSet<Block> secondaryPoi, @Nullable SoundEvent workSound)}，
 * 无 26.1 的 tradeSetsByLevel（TradeSet）参数；第一参数传 {@code modid:name} 字符串（26.1 为翻译组件）。
 * 交易注册请使用 1.21.1 机制：监听 {@code VillagerTradesEvent} 或直接操作 {@code VillagerTrades.TRADES}。
 */
public class VillagerProfessionBuilder<P> extends SelfBuilder<VillagerProfession, P, VillagerProfessionBuilder<P>> {
    public VillagerProfessionBuilder(AbstractRegistrum<?> owner,
                                     P parent,
                                     String name,
                                     BuilderCallback callback,
                                     Predicate<Holder<PoiType>> heldJobSite,
                                     Predicate<Holder<PoiType>> acquirableJobSite,
                                     ImmutableSet<Item> requestedItems,
                                     ImmutableSet<Block> secondaryPoi,
                                     @Nullable SoundEvent workSound) {
        super(owner, parent, name, callback,
                Registries.VILLAGER_PROFESSION, () -> new VillagerProfession(
                owner.getModid() + ":" + name,
                heldJobSite,
                acquirableJobSite,
                requestedItems,
                secondaryPoi,
                workSound
        ));
    }

    public VillagerProfessionBuilder(AbstractRegistrum<?> owner,
                                     P parent,
                                     String name,
                                     BuilderCallback callback,
                                     Predicate<Holder<PoiType>> heldJobSite,
                                     Predicate<Holder<PoiType>> acquirableJobSite,
                                     @Nullable SoundEvent workSound) {
        this(owner, parent, name, callback, heldJobSite, acquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), workSound);
    }



    public VillagerProfessionBuilder(AbstractRegistrum<?> owner,
                                     P parent,
                                     String name,
                                     BuilderCallback callback,
                                     ResourceKey<PoiType> jobSite,
                                     @Nullable SoundEvent workSound
                                     ) {
        this(owner, parent, name, callback, h -> h.is(jobSite), h -> h.is(jobSite), workSound);
    }
}
