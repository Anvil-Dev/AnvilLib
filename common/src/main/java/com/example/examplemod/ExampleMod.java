package com.example.examplemod;

import com.example.examplemod.api.registry.ExampleModRegistrate;
import com.example.examplemod.data.ExampleModDatagen;
import com.example.examplemod.init.ModBlocks;
import com.example.examplemod.init.ModCreativeModeTab;
import com.example.examplemod.init.ModItems;

public class ExampleMod {
    public static final String MOD_ID = "examplemod";

    public static final ExampleModRegistrate REGISTRATE = ExampleModRegistrate.create(MOD_ID);

    public static void init() {
        // common init
        ModItems.register();
        ModBlocks.register();
        ModCreativeModeTab.register();

        // datagen init
        ExampleModDatagen.init();

        // fabric exclusive, squeeze this in here to register before stuff is used
        REGISTRATE.registerRegistrate();
    }
}
