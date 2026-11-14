package dev.anvilcraft.lib.v2.rendering.preview.gui;

import net.minecraft.resources.ResourceLocation;

/** 验证提供器选择以及只读绘制作用域在嵌套和异常时的状态恢复。 */
public final class PreviewRegressionTest {
    private PreviewRegressionTest() {
    }

    public static void main(String[] args) {
        var first = ResourceLocation.fromNamespaceAndPath("test", "preview_first");
        var second = ResourceLocation.fromNamespaceAndPath("test", "preview_second");
        StructureGuiProviders.Provider provider = context -> null;
        require(StructureGuiProviders.get(first) == null, "未知方块应使用原生菜单路径");
        StructureGuiProviders.register(first, provider);
        require(StructureGuiProviders.get(first) == provider && StructureGuiProviders.get(second) == null,
            "提供器必须精确匹配方块标识");
        boolean rejected = false;
        try {
            StructureGuiProviders.register(first, context -> null);
        } catch (IllegalStateException exception) {
            rejected = true;
        }
        require(rejected && StructureGuiProviders.get(first) == provider, "重复注册不得覆盖已有提供器");
        require(!StructureGuiSession.isRendering(), "绘制作用域初始应关闭");
        StructureGuiSession.renderOnly(0.5f, () -> {
            require(StructureGuiSession.isRendering(), "进入作用域后应启用只读绘制");
            require(StructureGuiSession.tint(0x80FFFFFF) == 0x40FFFFFF, "应保留原色并相乘透明度");
            try {
                StructureGuiSession.renderOnly(0.25f, () -> {
                    require(StructureGuiSession.tint(0x80FFFFFF) == 0x20FFFFFF, "嵌套作用域应使用自己的透明度");
                    throw new IllegalArgumentException("验证异常退出");
                });
            } catch (IllegalArgumentException exception) {
                require(StructureGuiSession.isRendering() && StructureGuiSession.opacity() == 0.5f,
                    "异常退出后必须恢复外层作用域");
            }
        });
        require(!StructureGuiSession.isRendering() && StructureGuiSession.tint(0x80FFFFFF) == 0x80FFFFFF,
            "结束后不得影响普通界面绘制");
        float previous = StructureGuiGeometry.easeOut(0);
        for (int index = 1; index <= 100; index++) {
            float value = StructureGuiGeometry.easeOut(index / 100f);
            require(value >= previous && value <= 1, "浮窗过渡应单调且不越界");
            previous = value;
        }
        System.out.println("共享结构预览回归检查通过");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
