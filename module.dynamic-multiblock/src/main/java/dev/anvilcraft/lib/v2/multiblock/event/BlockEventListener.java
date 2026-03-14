package dev.anvilcraft.lib.v2.multiblock.event;

import dev.anvilcraft.lib.v2.multiblock.AnvilLibDynamicMultiblock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = AnvilLibDynamicMultiblock.MOD_ID)
public class BlockEventListener {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        event.getState().anvillib$unbind(event.getLevel());
    }
}
