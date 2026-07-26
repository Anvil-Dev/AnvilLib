package dev.anvilcraft.lib.v2.space_select.client;

import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import dev.anvilcraft.lib.v2.space_select.District;
import dev.anvilcraft.lib.v2.space_select.DistrictManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibSpaceSelect.MOD_ID, value = Dist.CLIENT)
@ApiStatus.Internal
public final class SpaceSelectScrollHandler {

    private SpaceSelectScrollHandler() {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        boolean ctrlDown = mc.hasControlDown();
        boolean altDown = mc.hasAltDown();
        if (!ctrlDown && !altDown) return;

        DistrictManager.DistrictKey mainHand = new DistrictManager.DistrictKey(
            mc.player.getInventory().getSelectedSlot(),
            false,
            mc.player.getMainHandItem().getItem()
        );
        DistrictManager.DistrictKey offHand = new DistrictManager.DistrictKey(-1, true, mc.player.getOffhandItem().getItem());
        DistrictManager.DistrictKey districtKey;
        if (mainHand.check(mc.player)) {
            districtKey = mainHand;
        } else if (offHand.check(mc.player)) {
            districtKey = offHand;
        } else {
            return;
        }

        District district = AnvilLibSpaceSelectClient.MANAGER.getDistrictMap().get(districtKey);
        if (district == null) return;

        double scrollY = event.getScrollDeltaY();
        if (scrollY == 0) return;
        int scrollAmount = (int) Math.signum(scrollY);

        Vec3 lookAngle = mc.player.getViewVector(1.0F);
        Vec3 playerPos = mc.player.position();
        AABB boundingBox = mc.player.getBoundingBox();

        if (ctrlDown) {
            Direction.Axis axis = District.getPrimaryAxis(lookAngle);
            district.scaleOnAxis(axis, scrollAmount, playerPos, boundingBox, lookAngle);
            event.setCanceled(true);
        } else {
            Direction dir = Direction.getApproximateNearest(lookAngle.x, lookAngle.y, lookAngle.z);
            district.move(dir, scrollAmount);
            event.setCanceled(true);
        }
    }
}
