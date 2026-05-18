package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.ButtonComponent;
import dev.anvilcraft.lib.v2.ui.component.BoxComponent;
import dev.anvilcraft.lib.v2.ui.component.BoxScope;
import dev.anvilcraft.lib.v2.ui.component.ColumnComponent;
import dev.anvilcraft.lib.v2.ui.component.ColumnScope;
import dev.anvilcraft.lib.v2.ui.component.RowComponent;
import dev.anvilcraft.lib.v2.ui.component.RowScope;
import dev.anvilcraft.lib.v2.ui.component.TextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base scope for building component trees.
 * <p>
 * Container components (Column, Row, Box) create a scope,
 * run their content lambda against it, then collect the children.
 * <p>
 * Component builders are defined here as concrete methods
 * so all scope subclasses inherit them.
 */
public abstract class UIScope {

    final List<UIComponent> children = new ArrayList<>();

    public void addChild(UIComponent child) {
        children.add(child);
    }

    public List<UIComponent> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** Internal: clear children before recomposition. */
    void clearChildren() {
        children.clear();
    }

    // ── component builders ──

    public TextComponent Text(String text) {
        TextComponent c = new TextComponent(Modifier.NONE, text);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    public ButtonComponent Button(String label, Runnable onClick) {
        ButtonComponent c = new ButtonComponent(Modifier.NONE, label, onClick);
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    public ColumnComponent Column(Consumer<ColumnScope> content) {
        ColumnComponent c = new ColumnComponent(Modifier.NONE);
        ColumnScope inner = new ColumnScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    public RowComponent Row(Consumer<RowScope> content) {
        RowComponent c = new RowComponent(Modifier.NONE);
        RowScope inner = new RowScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }

    public BoxComponent Box(Consumer<BoxScope> content) {
        BoxComponent c = new BoxComponent(Modifier.NONE);
        BoxScope inner = new BoxScope();
        content.accept(inner);
        c.setChildren(inner.getChildren());
        addChild(c);
        Composition.current().emit(c);
        return c;
    }
}
