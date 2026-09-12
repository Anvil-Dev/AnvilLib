package dev.anvilcraft.lib.v2.test.multiblock.init;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class LibMultiblocks {
    public static final ResourceKey<MultiblockDefinition> SIMPLE = key(AnvilLibTest.of("simple"));
    public static final ResourceKey<MultiblockDefinition> COMPLICATED = key(AnvilLibTest.of("complicated"));
    public static final ResourceKey<MultiblockDefinition> WAHT = key(AnvilLibTest.of("waht"));

    public static void bootstrap(BootstrapContext<MultiblockDefinition> ctx) {
        ctx.register(
            SIMPLE,
            MultiblockDefinition.builder()
                .addController(Blocks.FURNACE)
                .add(new Vec3i(0, 1, 0), Blocks.IRON_BLOCK)
                .build()
        );
        ctx.register(
            COMPLICATED,
            MultiblockDefinition.builder()
                .addController(LibBlocks.TEST_CONTROLLER.get())
                .add(new Vec3i(0, -1, 0), Blocks.IRON_BLOCK)
                .build()
        );
        ctx.register(
            WAHT,
            MultiblockDefinition.seriaBuilder()
                .layer("A")
                .layer("0")
                .mapController(Blocks.WHITE_BED)
                .map('A', Blocks.IRON_BLOCK)
                .build()
        );
    }

    private static ResourceKey<MultiblockDefinition> key(ResourceLocation id) {
        return ResourceKey.create(LibRegistries.DEFINITIONS_KEY, id);
    }
}
