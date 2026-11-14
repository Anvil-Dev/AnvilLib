package dev.anvilcraft.lib.v2.rendering.preview.gui;

/** 将提取时的浮窗透明度保留到物品图集和大尺寸物品的最终绘制阶段。 */
public interface StructureGuiItemOpacity {
    float anvillib$opacity();

    static int premultipliedColor(float opacity) {
        int channel = Math.clamp(Math.round(opacity * 255), 0, 255);
        return channel << 24 | channel << 16 | channel << 8 | channel;
    }
}
