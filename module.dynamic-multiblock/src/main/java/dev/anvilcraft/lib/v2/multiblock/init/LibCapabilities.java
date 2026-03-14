package dev.anvilcraft.lib.v2.multiblock.init;

import dev.anvilcraft.lib.v2.multiblock.AnvilLibDynamicMultiblock;
import dev.anvilcraft.lib.v2.multiblock.part.IMultiPart;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class LibCapabilities {
    public static final BlockCapability<IMultiPart, Void> MULTI_PART_CAPABILITY = BlockCapability.createVoid(
        AnvilLibDynamicMultiblock.of("multipart"),
        IMultiPart.class
    );
}
