package com.example.examplemod.forge;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.fml.common.Mod;

@Mod(ExampleMod.MOD_ID)
public class AnvilCraftForge {
    public AnvilCraftForge() {
        ExampleMod.init();
    }
}