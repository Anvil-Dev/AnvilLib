package dev.anvilcraft.lib.v2.multiblock.init;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.multiblock.AnvilLibDynamicMultiblock;
import dev.anvilcraft.lib.v2.multiblock.definition.MultiblockDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = AnvilLibDynamicMultiblock.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class LibRegistries {
    public static final ResourceKey<Registry<MultiblockDefinition>> DEFINITION_KEY = ResourceKey.createRegistryKey(
        AnvilLibDynamicMultiblock.of("trigger")
    );

    @SubscribeEvent
    public static void registerDataRegistries(DataPackRegistryEvent.NewRegistry event) {
        MapCodec<MultiblockDefinition> codec = MultiblockDefinition.CODEC;
        event.dataPackRegistry(DEFINITION_KEY, codec.codec(), codec.codec());
    }
}
