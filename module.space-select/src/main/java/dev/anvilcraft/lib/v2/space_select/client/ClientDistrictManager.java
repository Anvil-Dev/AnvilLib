package dev.anvilcraft.lib.v2.space_select.client;

import dev.anvilcraft.lib.v2.space_select.District;
import dev.anvilcraft.lib.v2.space_select.DistrictManager;
import dev.anvilcraft.lib.v2.space_select.SpaceSelectItem;
import dev.anvilcraft.lib.v2.space_select.network.payload.SpaceSelectPayload;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class ClientDistrictManager extends DistrictManager {
    private final District tempDistrict = new District(new BlockPos.MutableBlockPos(), new BlockPos.MutableBlockPos());
    private final Map<ItemStack, BlockPos> selectingMap = new HashMap<>();

    public boolean isSelecting(ItemStack stack) {
        return this.selectingMap.containsKey(stack);
    }

    public void startSelect(ItemStack stack, BlockPos blockPos) {
        this.selectingMap.put(stack, blockPos);
    }

    public void endSelect(ItemStack stack, BlockPos blockPos) {
        BlockPos start = this.selectingMap.remove(stack);
        if (start != null) {
            this.select(stack, District.create(start, blockPos));
            SpaceSelectPayload payload = new SpaceSelectPayload(
                !(Objects.requireNonNull(Minecraft.getInstance().player).getMainHandItem().getItem() instanceof SpaceSelectItem),
                start,
                blockPos
            );
            ClientPacketDistributor.sendToServer(payload);
        }
    }

    @Override
    public void clear(ItemStack stack) {
        super.clear(stack);
        this.selectingMap.remove(stack);
    }

    public @Nullable District getTempDistrict() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return null;
        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return null;
        BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offhandItem = player.getOffhandItem();
        if (
            !(mainHandItem.getItem() instanceof SpaceSelectItem)
            && !(offhandItem.getItem() instanceof SpaceSelectItem)
        ) {
            return null;
        }
        BlockPos pos;
        if (this.isSelecting(mainHandItem)) {
            pos = this.getSelectingMap().get(mainHandItem);
        } else {
            pos = this.getSelectingMap().get(offhandItem);
        }
        if (pos == null) return null;
        this.tempDistrict.start()
            .set(Math.min(pos.getX(), hitPos.getX()), Math.min(pos.getY(), hitPos.getY()), Math.min(pos.getZ(), hitPos.getZ()));
        this.tempDistrict.end()
            .set(Math.max(pos.getX(), hitPos.getX()), Math.max(pos.getY(), hitPos.getY()), Math.max(pos.getZ(), hitPos.getZ()));
        return this.tempDistrict;
    }
}
