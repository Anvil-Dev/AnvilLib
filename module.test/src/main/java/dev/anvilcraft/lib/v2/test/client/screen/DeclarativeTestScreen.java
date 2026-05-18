package dev.anvilcraft.lib.v2.test.client.screen;

import dev.anvilcraft.lib.v2.ui.*;
import dev.anvilcraft.lib.v2.ui.component.TextComponent;
import net.minecraft.network.chat.Component;

/**
 * 端到端测试 Screen，展示声明式 UI 的全部 Phase 3+4 功能：
 * Column / Row / Box 布局，Text / Button / Spacer 组件，状态管理。
 */
public class DeclarativeTestScreen extends DeclarativeScreen {

    public DeclarativeTestScreen() {
        super(Component.literal("Declarative UI Test"));
    }

    @Override
    protected void content(UIScope scope) {
        MutableState<Integer> counter = Composition.current()
                .remember(() -> new MutableState<>(0));

        scope.Column(col -> {
            col.Text("Declarative UI Demo")
                    .color(0xFFFFFF00)
                    .shadow(false);

            col.Spacer(0, 4);

            // Row: 横向按钮栏
            col.Row(row -> {
                row.Button("-", () -> counter.setValue(counter.getValue() - 1));
                row.Text("  " + counter.getValue() + "  ")
                        .align(TextComponent.Align.CENTER);
                row.Button("+", () -> counter.setValue(counter.getValue() + 1));
            }).spacing(4);

            col.Spacer(0, 8);

            // Box: 叠加
            col.Box(box -> {
                box.Text("          ");
                box.Text("Count: " + counter.getValue())
                        .color(0xFF00FF00)
                        .shadow(false);
            }).contentAlignment(Alignment.Horizontal.Center, Alignment.Vertical.Center);

            col.Spacer(0, 8);

            col.Button("Reset", () -> counter.setValue(0));
        }).spacing(8);
    }
}

