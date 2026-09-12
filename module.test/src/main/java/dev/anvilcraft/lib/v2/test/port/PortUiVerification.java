package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.registrum.client.gui.CreativeVariantPickerOverlay;
import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.anvilcraft.lib.v2.test.all.TestItemGroups;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;

/** 在测试世界中实际绘制分区与叠加层，并经过原版创造物品点击路径验证选择。 */
public final class PortUiVerification {
    private static CreativeModeInventoryScreen screen;
    private static Slot source;

    public static void open() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        CreativeVariantPickerRegistry.setVanillaColorVariantPickerEnabled(() -> true);
        screen = new CreativeModeInventoryScreen(minecraft.player, minecraft.level.enabledFeatures(), true);
        minecraft.setScreen(screen);
        CreativeModeTab tab = TestItemGroups.TEST_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(), true, minecraft.level.registryAccess()));
        var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
        select.setAccessible(true);
        select.invoke(screen, tab);
        source = screen.getMenu().slots.stream().filter(slot -> slot.getItem().is(Items.WHITE_WOOL)).findFirst().orElseThrow();
        screen.mouseClicked(sourceClick(), false);
        screen.mouseReleased(sourceClick());
        PortVerification.check(overlay() != null, "右键打开实际创造栏叠加层");
        PortVerification.check(tab.getSearchTabDisplayItems().stream().filter(stack -> stack.getItem().toString().endsWith("_wool")).count() == 16,
            "创造搜索保留全部十六色物品");
    }

    private static MouseButtonEvent sourceClick() {
        return new MouseButtonEvent(screen.getGuiLeft() + source.x + 8, screen.getGuiTop() + source.y + 8, new MouseButtonInfo(1, 0));
    }

    private static CreativeVariantPickerOverlay overlay() throws Exception {
        var field = CreativeModeInventoryScreen.class.getDeclaredField("anvillib$variantOverlay");
        field.setAccessible(true);
        return (CreativeVariantPickerOverlay) field.get(screen);
    }

    public static void finish() throws Exception {
        var leftMethod = CreativeVariantPickerOverlay.class.getDeclaredMethod("left", int.class);
        var topMethod = CreativeVariantPickerOverlay.class.getDeclaredMethod("top", int.class);
        leftMethod.setAccessible(true);
        topMethod.setAccessible(true);
        int left = (int) leftMethod.invoke(overlay(), screen.getGuiLeft());
        int top = (int) topMethod.invoke(overlay(), screen.getGuiTop());
        PortVerification.check(top >= 0 && top + 80 <= Minecraft.getInstance().getWindow().getGuiScaledHeight(), "叠加层不能超出小窗口");
        var click = new MouseButtonEvent(left + 30, top + 12, new MouseButtonInfo(0, 0));
        PortVerification.check(screen.mouseClicked(click, false), "选择点击被叠加层接管");
        PortVerification.check(screen.mouseReleased(click), "选择后的松开事件被消费");
        PortVerification.check(screen.getMenu().getCarried().is(Items.LIGHT_GRAY_WOOL), "选择结果进入真实创造栏携带栈");
        screen.mouseClicked(sourceClick(), false);
        screen.mouseReleased(sourceClick());
        PortVerification.check(overlay() == null, "再次右键源槽关闭叠加层");
        screen.mouseClicked(sourceClick(), false);
        screen.mouseReleased(sourceClick());
        screen.mouseScrolled(0, 0, 0, -1);
        PortVerification.check(overlay() == null, "滚动列表关闭叠加层");
    }
}
