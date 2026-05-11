package dev.anvilcraft.lib.v2.registrum.util.entry.data;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AttachmentEntry<E> extends RegistryEntry<AttachmentType<?>, AttachmentType<E>> {

    public AttachmentEntry(AbstractRegistrum<?> owner, DeferredHolder<AttachmentType<?>, AttachmentType<E>> key) {
        super(owner, key);
    }

    public static <E> AttachmentEntry<E> cast(RegistryEntry<AttachmentType<?>, AttachmentType<E>> entry) {
        return RegistryEntry.cast(AttachmentEntry.class, entry);
    }
}
