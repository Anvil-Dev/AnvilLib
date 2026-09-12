package dev.anvilcraft.lib.v2.registrum.attachment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.Nullable;

/** 1.21.4 附件同步处理器；initialSync 为 true 时必须写入完整数据。 */
public interface AttachmentSyncHandler<T> {
    default boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer player) { return true; }

    /** 不写入任何字节时跳过本次更新。 */
    void write(RegistryFriendlyByteBuf buffer, T attachment, boolean initialSync);

    /** 返回 null 会移除客户端附件。 */
    @Nullable T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buffer, @Nullable T previousValue);
}
