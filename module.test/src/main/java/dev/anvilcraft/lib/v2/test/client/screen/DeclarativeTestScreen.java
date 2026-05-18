package dev.anvilcraft.lib.v2.test.client.screen;

import dev.anvilcraft.lib.v2.ui.Alignment;
import dev.anvilcraft.lib.v2.ui.Composition;
import dev.anvilcraft.lib.v2.ui.DeclarativeScreen;
import dev.anvilcraft.lib.v2.ui.ForEach;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.Ref;
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
        Ref<Integer> counter = comp.ref(0);
        Ref<Boolean> checked = comp.ref(false);
        Ref<String> text = comp.ref("");

        scope.Scrollable(
            this.height, scroll -> {
                scroll.Column(col -> {
                    // ── 标题 ──
                    col.Text("=== AnvilLib Declarative UI ===").color(0xFFFFFF00);

                    // ── 1. Text 样式 ──
                    col.Text("1) Text styles:");
                    col.Row(row -> {
                        row.Text("Shadow").shadow(true).color(0xFFFFAA00);
                        row.Text(" | Left").align(TextComponent.Align.LEFT);
                        row.Text("Center").align(TextComponent.Align.CENTER).color(0xFF00AAFF);
                        row.Text("Right |").align(TextComponent.Align.RIGHT);
                    }).spacing(8);

                    col.Spacer(0, 4);

                    // ── 2. Button + 状态 ──
                    col.Text("2) Button + state (Counter):");
                    col.Row(row -> {
                        row.Button("-", () -> counter.setValue(counter.getValue() - 1));
                        row.Text("  " + counter.getValue() + "  ").align(TextComponent.Align.CENTER);
                        row.Button("+", () -> counter.setValue(counter.getValue() + 1));
                        row.Button("Reset", () -> counter.setValue(0));
                    }).spacing(4);

                    col.Spacer(0, 4);

                    // ── 3. Box 层叠 ──
                    col.Text("3) Box overlay:");
                    col.Box(box -> {
                        box.Text("                    ");
                        box.Text("<< Overlay Text >>").color(0xFF00FF00);
                    }).contentAlignment(Alignment.Horizontal.Center, Alignment.Vertical.Center);

                    col.Spacer(0, 4);

                    // ── 4. Grid 网格 ──
                    col.Text("4) Grid (3 columns):");
                    col.Grid(
                        3, grid -> {
                            for (int i = 0; i < 6; i++) {
                                grid.Button(
                                    "G" + i, () -> {
                                    }
                                );
                            }
                        }
                    ).spacing(2, 2);

                    col.Spacer(0, 4);

                    // ── 5. Checkbox ──
                    col.Text("5) Checkbox:");
                    col.Row(row -> {
                        row.Checkbox("Enable feature", checked.getValue(), () -> checked.setValue(!checked.getValue()));
                        row.Text("  Enabled: " + checked.getValue());
                    }).spacing(4);

                    col.Spacer(0, 4);

                    // ── 6. Slider ──
                    Ref<Float> sliderVal = comp.remember(() -> new Ref<>(50f));
                    col.Text("6) Slider:");
                    col.Row(row -> {
                        row.Slider(sliderVal.getValue(), 0, 100, 100, sliderVal::setValue);
                        row.Text(" " + sliderVal.getValue().intValue() + "%");
                    }).spacing(4);

                    col.Spacer(0, 4);

                    // ── 7. TextInput ──
                    col.Text("7) TextInput:");
                    col.Row(row -> {
                        row.TextInput("Enter text...", text::setValue);
                        row.Text("  Value: '" + text.getValue() + "'");
                    }).spacing(4);

                    col.Spacer(0, 4);

                    // ── 8. ForEach ──
                    col.Text("8) ForEach (list of 4 items):");
                    ForEach.of(col, List.of("Apple", "Banana", "Cherry", "Date"), (s, item) -> s.Text("  - " + item));

                    col.Spacer(0, 4);

                    // ── 9. Modifier 样式 ──
                    col.Text("9) Modifiers:");
                    col.Row(row -> {
                        row.Button(
                            "Styled", () -> {
                            }
                        ).modifier(Modifier.NONE.background(0xFF884444).border(1, 0xFFFF8888));
                        row.Text("  ");
                        row.Text("Padded").modifier(Modifier.NONE.padding(8).background(0xFF444488));
                    }).spacing(4);

                }).spacing(6).modifier(Modifier.NONE.padding(10));
            }
        );
    }
}

