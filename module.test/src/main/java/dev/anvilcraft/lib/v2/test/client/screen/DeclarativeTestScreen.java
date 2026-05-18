package dev.anvilcraft.lib.v2.test.client.screen;

import dev.anvilcraft.lib.v2.ui.Alignment;
import dev.anvilcraft.lib.v2.ui.Composition;
import dev.anvilcraft.lib.v2.ui.DeclarativeScreen;
import dev.anvilcraft.lib.v2.ui.ForEach;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.MutableState;
import dev.anvilcraft.lib.v2.ui.UIScope;
import dev.anvilcraft.lib.v2.ui.component.TextComponent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 声明式 UI 全面测试 Screen。覆盖 Phase 1-8 所有组件和功能。
 */
public class DeclarativeTestScreen extends DeclarativeScreen {

    public DeclarativeTestScreen() {
        super(Component.literal("Declarative UI Test"));
    }

    @Override
    protected void content(UIScope scope) {
        Composition comp = Composition.current();
        MutableState<Integer> counter = comp.remember(() -> new MutableState<>(0));
        MutableState<Boolean> checked = comp.remember(() -> new MutableState<>(false));
        MutableState<String> text = comp.remember(() -> new MutableState<>(""));

        scope.Column(col -> {
            col.Scrollable(
                this.height, scrollableScope -> {
                    // ── 标题 ──
                    scrollableScope.Text("=== AnvilLib Declarative UI ===").color(0xFFFFFF00);

                    // ── 1. Text 样式 ──
                    scrollableScope.Text("1) Text styles:");
                    scrollableScope.Row(row -> {
                        row.Text("Shadow").shadow(true).color(0xFFFFAA00);
                        row.Text(" | Left").align(TextComponent.Align.LEFT);
                        row.Text("Center").align(TextComponent.Align.CENTER).color(0xFF00AAFF);
                        row.Text("Right|").align(TextComponent.Align.RIGHT);
                    }).spacing(8);

                    scrollableScope.Spacer(0, 4);

                    // ── 2. Button + 状态 ──
                    scrollableScope.Text("2) Button + state (Counter):");
                    scrollableScope.Row(row -> {
                        row.Button("-", () -> counter.setValue(counter.getValue() - 1));
                        row.Text("  " + counter.getValue() + "  ").align(TextComponent.Align.CENTER);
                        row.Button("+", () -> counter.setValue(counter.getValue() + 1));
                        row.Button("Reset", () -> counter.setValue(0));
                    }).spacing(4);

                    scrollableScope.Spacer(0, 4);

                    // ── 3. Box 层叠 ──
                    scrollableScope.Text("3) Box overlay:");
                    scrollableScope.Box(box -> {
                        box.Text("                    ");
                        box.Text("<< Overlay Text >>").color(0xFF00FF00);
                    }).contentAlignment(Alignment.Horizontal.Center, Alignment.Vertical.Center);

                    scrollableScope.Spacer(0, 4);

                    // ── 4. Grid 网格 ──
                    scrollableScope.Text("4) Grid (3 columns):");
                    scrollableScope.Grid(
                        3, grid -> {
                            for (int i = 0; i < 6; i++) {
                                grid.Button(
                                    "G" + i, () -> {
                                    }
                                );
                            }
                        }
                    ).spacing(2, 2);

                    scrollableScope.Spacer(0, 4);

                    // ── 5. Checkbox ──
                    scrollableScope.Text("5) Checkbox:");
                    scrollableScope.Row(row -> {
                        row.Checkbox("Enable feature", checked.getValue(), () -> checked.setValue(!checked.getValue()));
                        row.Text("  Enabled: " + checked.getValue());
                    }).spacing(4);

                    scrollableScope.Spacer(0, 4);

                    // ── 6. Slider ──
                    MutableState<Float> sliderVal = comp.remember(() -> new MutableState<>(50f));
                    scrollableScope.Text("6) Slider:");
                    scrollableScope.Row(row -> {
                        row.Slider(sliderVal.getValue(), 0, 100, 100, v -> sliderVal.setValue(v));
                        row.Text(" " + sliderVal.getValue().intValue() + "%");
                    }).spacing(4);

                    scrollableScope.Spacer(0, 4);

                    // ── 7. TextField ──
                    scrollableScope.Text("7) TextField:");
                    scrollableScope.Row(row -> {
                        row.TextField("Type here...", v -> text.setValue(v));
                        row.Text("  Value: '" + text.getValue() + "'");
                    }).spacing(4);

                    scrollableScope.Spacer(0, 4);

                    // ── 8. ForEach ──
                    scrollableScope.Text("8) ForEach (list of 4 items):");
                    ForEach.of(scrollableScope, List.of("Apple", "Banana", "Cherry", "Date"), (s, item) -> s.Text("  - " + item));

                    scrollableScope.Spacer(0, 4);

                    // ── 9. Modifier 样式 ──
                    scrollableScope.Text("9) Modifiers:");
                    scrollableScope.Row(row -> {
                        row.Button(
                            "Styled", () -> {
                            }
                        ).modifier(Modifier.NONE.background(0xFF884444).border(1, 0xFFFF8888));
                        row.Text("  ");
                        row.Text("Padded").modifier(Modifier.NONE.padding(8).background(0xFF444488));
                    }).spacing(4);
                }
            );
        }).spacing(6).modifier(Modifier.NONE.padding(10));
    }
}

