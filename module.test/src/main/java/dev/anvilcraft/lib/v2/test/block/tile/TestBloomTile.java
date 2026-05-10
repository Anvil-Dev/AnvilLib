package dev.anvilcraft.lib.v2.test.block.tile;

import dev.anvilcraft.lib.v2.sync.annotation.Sync;
import dev.anvilcraft.lib.v2.sync.management.SyncProxy;
import dev.anvilcraft.lib.v2.test.all.TestTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Sync
public class TestBloomTile extends BlockEntity {
    public final SyncProxy<Integer> aaa = new SyncProxy<>(0);

    public TestBloomTile(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    public void test() {
    }
}
