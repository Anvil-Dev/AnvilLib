package dev.anvilcraft.lib.v2.multiblock.init;

import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblock;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibMultiblock.MOD_ID)
public class LibRegistries {
    public static final ResourceKey<Registry<MultiblockDefinition>> DEFINITIONS_KEY = ResourceKey.createRegistryKey(
        AnvilLibMultiblock.of("definitions")
    );

    @ApiStatus.Internal
    @SubscribeEvent
    public static void registerDataRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(DEFINITIONS_KEY, MultiblockDefinition.CODEC.codec(), MultiblockDefinition.CODEC.codec());
    }
}
