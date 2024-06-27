package com.example.examplemod.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.level.block.Block;

import static com.example.examplemod.ExampleMod.REGISTRATE;

public class ModBlocks {

    static {
        REGISTRATE.creativeModeTab(ModCreativeModeTab.EXAMPLE_TAB);
    }

    public static final RegistryEntry<Block> EXAMPLE_BLOCK = REGISTRATE
            .block("example_block", Block::new).simpleItem().register();

    public static void register() {

    }
}
