package dev.anvilcraft.lib.v2.registrum.builders.data;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.data.AttachmentEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.*;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AttachmentBuilder<E, P> extends AbstractBuilder<AttachmentType<?>, AttachmentType<E>, P, AttachmentBuilder<E, P>> {

    private final AttachmentType.Builder<E> builder;

    public AttachmentBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, Function<IAttachmentHolder, E> const_) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.ATTACHMENT_TYPES);
        this.builder = AttachmentType.builder(const_);
    }
    public AttachmentBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, Supplier<E> const_) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.ATTACHMENT_TYPES);
        this.builder = AttachmentType.builder(const_);
    }

    public AttachmentBuilder<E, P> serialize(IAttachmentSerializer<E> serializer) {
        builder.serialize(serializer);
        return this;
    }
    public AttachmentBuilder<E, P> serialize(MapCodec<E> serializer) {
        builder.serialize(serializer);
        return this;
    }
    public AttachmentBuilder<E, P> serialize(MapCodec<E> serializer, Predicate<? super E> predicate) {
        builder.serialize(serializer, predicate);
        return this;
    }

    public AttachmentBuilder<E, P> copyOnDeath() {
        builder.copyOnDeath();
        return this;
    }

    public AttachmentBuilder<E, P> copyHandler(IAttachmentCopyHandler<E> cloner) {
        builder.copyHandler(cloner);
        return this;
    }
    public AttachmentBuilder<E, P> sync(AttachmentSyncHandler<E> syncHandler) {
        builder.sync(syncHandler);
        return this;
    }
    public AttachmentBuilder<E, P> sync(StreamCodec<? super RegistryFriendlyByteBuf, E> streamCodec) {
        builder.sync(streamCodec);
        return this;
    }
    public AttachmentBuilder<E, P> sync(BiPredicate<IAttachmentHolder, ServerPlayer> sendToPlayer, StreamCodec<? super RegistryFriendlyByteBuf, E> streamCodec) {
        builder.sync(sendToPlayer, streamCodec);
        return this;
    }

    @Override
    public AttachmentEntry<E> register() {
        return (AttachmentEntry<E>) super.register();
    }

    @Override
    protected AttachmentEntry<E> createEntryWrapper(DeferredHolder<AttachmentType<?>, AttachmentType<E>> delegate) {
        return new AttachmentEntry<>(getOwner(), delegate);
    }

    @Override
    protected AttachmentType<E> createEntry() {
        return builder.build();
    }
}
