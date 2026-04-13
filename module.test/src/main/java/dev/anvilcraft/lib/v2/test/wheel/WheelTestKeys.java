package dev.anvilcraft.lib.v2.test.wheel;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID, value = Dist.CLIENT)
public final class WheelTestKeys {
    public static final String CATEGORY = "key.categories.anvillib_test.wheel";

    /** 点按模式：按一下打开，鼠标选择后点击触发 */
    public static final KeyMapping TAP_KEY = new KeyMapping(
        "key.anvillib_test.wheel_tap",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    /** 长按模式：按住显示，松开触发当前选中项 */
    public static final KeyMapping HOLD_KEY = new KeyMapping(
        "key.anvillib_test.wheel_hold",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        CATEGORY
    );

    private WheelTestKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TAP_KEY);
        event.register(HOLD_KEY);
    }
}
