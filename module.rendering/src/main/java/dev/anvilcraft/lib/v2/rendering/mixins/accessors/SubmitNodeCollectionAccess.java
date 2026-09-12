package dev.anvilcraft.lib.v2.rendering.mixins.accessors;

import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SubmitNodeCollection.class)
public interface SubmitNodeCollectionAccess {

    @Accessor("wasUsed")
    void alrSetWasUsed(boolean value);
}
