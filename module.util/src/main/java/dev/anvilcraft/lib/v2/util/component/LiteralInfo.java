package dev.anvilcraft.lib.v2.util.component;

import net.minecraft.network.chat.Component;

public record LiteralInfo(String value) implements IComponentInfo {
    @Override
    public void addInto(MultilineComponentHelper helper) {
        helper.addln(Component.literal(this.value));
    }
}
