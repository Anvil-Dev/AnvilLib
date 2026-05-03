package dev.anvilcraft.lib.v2.util.component;

import net.minecraft.network.chat.Component;

public record DirectInfo(Component value) implements IComponentInfo {
    @Override
    public void addInto(MultilineComponentHelper helper) {
        helper.addln(this.value);
    }
}
