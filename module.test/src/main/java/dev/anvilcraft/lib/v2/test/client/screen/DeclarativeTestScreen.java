package dev.anvilcraft.lib.v2.test.client.screen;

import dev.anvilcraft.lib.v2.ui.*;
import net.minecraft.network.chat.Component;

/**
 * End-to-end test screen for the declarative UI system.
 * Demonstrates state management, recomposition, Column layout,
 * Text rendering, and Button interaction.
 */
public class DeclarativeTestScreen extends DeclarativeScreen {

    public DeclarativeTestScreen() {
        super(Component.literal("Declarative UI Test"));
    }

    @Override
    protected void content(UIScope scope) {
        // State that survives recomposition
        MutableState<Integer> counter = Composition.current()
                .remember(() -> new MutableState<>(0));

        scope.Column(col -> {
            col.Text("AnvilLib Declarative UI")
                    .color(0xFFFFFF00);

            col.Text("")
                    .text("Counter: " + counter.getValue());

            col.Button("Increment", () ->
                    counter.setValue(counter.getValue() + 1));

            col.Button("Decrement", () ->
                    counter.setValue(counter.getValue() - 1));

            col.Button("Reset", () ->
                    counter.setValue(0));
        });
    }
}
