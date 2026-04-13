package dev.anvilcraft.lib.v2.test.multiblock.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.test.multiblock.block.TestControllerBlock;

import static dev.anvilcraft.lib.v2.test.AnvilLibTest.REGISTRUM;

public class LibBlocks {
    static {
        REGISTRUM.defaultCreativeTab(LibItemGroups.TEST_TAB.getKey());
    }

    public static final BlockEntry<TestControllerBlock> TEST_CONTROLLER = REGISTRUM
        .block("test_controller", TestControllerBlock::new)
        .blockstate((ctx, provider) -> {
        })
        .item()
        .model((ctx, provider) -> {
        })
        .build()
        .register();

    public static void init() {
    }
}
