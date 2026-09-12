package dev.anvilcraft.lib.v2.registrum.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.anvilcraft.lib.v2.registrum.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AttachmentHolder.class)
abstract class AttachmentHolderMixin {
    @Shadow abstract IAttachmentHolder getExposedHolder();

    @WrapMethod(method = "getData(Lnet/neoforged/neoforge/attachment/AttachmentType;)Ljava/lang/Object;")
    private <T> T anvillib$create(AttachmentType<T> type, Operation<T> original) {
        boolean existed = ((IAttachmentHolder) this).hasData(type);
        T value = original.call(type);
        if (!existed) AttachmentSync.syncData(this.getExposedHolder(), type, true);
        return value;
    }

    @WrapMethod(method = "setData(Lnet/neoforged/neoforge/attachment/AttachmentType;Ljava/lang/Object;)Ljava/lang/Object;")
    private <T> T anvillib$set(AttachmentType<T> type, T value, Operation<T> original) {
        T previous = original.call(type, value);
        AttachmentSync.syncData(this.getExposedHolder(), type, previous == null);
        return previous;
    }

    @WrapMethod(method = "removeData(Lnet/neoforged/neoforge/attachment/AttachmentType;)Ljava/lang/Object;")
    private <T> T anvillib$remove(AttachmentType<T> type, Operation<T> original) {
        T previous = original.call(type);
        if (previous != null) AttachmentSync.syncData(this.getExposedHolder(), type, false);
        return previous;
    }
}
