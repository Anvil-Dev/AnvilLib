package dev.anvilcraft.lib.v2.test.all;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.block.tile.TestBloomTile;
import dev.anvilcraft.lib.v2.test.block.tile.TestCachedRenderingTile;
import dev.anvilcraft.lib.v2.test.block.tile.TestOcclusionTile;
import dev.anvilcraft.lib.v2.test.client.tesr.TestBloomTESR;
import dev.anvilcraft.lib.v2.test.client.tesr.TestOcclusionTESR;

public class TestTiles {
    public static final BlockEntityEntry<TestBloomTile> TEST_BLOOM = AnvilLibTest.REGISTRUM
        .blockEntity("test_bloom", TestBloomTile::new)
        .validBlock(TestBlocks.TEST_BLOOM)
        .renderer(() -> TestBloomTESR::new)
        .register();

    public static final BlockEntityEntry<TestOcclusionTile> TEST_OCCLUSION = AnvilLibTest.REGISTRUM
        .blockEntity("test_occlusion", TestOcclusionTile::new)
        .validBlock(TestBlocks.TEST_OCCLUSION)
        .renderer(() -> TestOcclusionTESR::new)
        .register();

    public static final BlockEntityEntry<TestCachedRenderingTile> TEST_CACHED_RENDERING = AnvilLibTest.REGISTRUM
        .blockEntity("test_cached_rendering", TestCachedRenderingTile::new)
        .validBlock(TestBlocks.TEST_CACHED_RENDERING)
        .register();

    public static void setupRegistration() {
    }
}
