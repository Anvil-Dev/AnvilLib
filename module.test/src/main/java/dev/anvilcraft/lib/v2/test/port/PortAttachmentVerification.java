package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.registrum.util.entry.data.AttachmentEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 附件兼容层的真实客户端负载回归，覆盖四类持有者与发送过滤。 */
public final class PortAttachmentVerification {
    public static final AttachmentEntry<Integer> VALUE = AnvilLibTest.REGISTRUM
        .attachment("port_attachment", () -> 17).sync(ByteBufCodecs.INT).register();
    public static final AttachmentEntry<Integer> HIDDEN = AnvilLibTest.REGISTRUM
        .attachment("port_hidden_attachment", () -> 19).sync((holder, player) -> false, ByteBufCodecs.INT).register();
    private static final BlockPos POSITION = new BlockPos(10, -60, 10);
    private static CompletableFuture<?> operation;
    private static int stage;
    private static int ticks;

    public static void init() { }

    public static boolean tick() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (operation != null) {
            if (!operation.isDone()) return false;
            operation.join();
        }
        if (++ticks < 20) return false;
        ticks = 0;
        if (stage == 0) {
            operation = server.submit(() -> server.overworld().setBlockAndUpdate(POSITION, Blocks.CHEST.defaultBlockState()));
        } else if (stage == 1) {
            operation = server.submit(() -> {
                var level = server.overworld();
                List<IAttachmentHolder> holders = List.of(server.getPlayerList().getPlayers().getFirst(), level,
                    level.getChunkAt(POSITION), level.getBlockEntity(POSITION));
                for (IAttachmentHolder holder : holders) {
                    holder.setData(VALUE.get(), 73);
                    holder.setData(HIDDEN.get(), 79);
                }
            });
        } else if (stage == 2 || stage == 3 || stage == 4) {
            var level = minecraft.level;
            List<IAttachmentHolder> holders = List.of(minecraft.player, level, level.getChunkAt(POSITION), level.getBlockEntity(POSITION));
            for (IAttachmentHolder holder : holders) {
                PortVerification.check(!holder.hasData(HIDDEN.get()), "发送过滤必须阻止附件负载");
                PortVerification.check(stage == 3 ? !holder.hasData(VALUE.get())
                    : holder.getExistingData(VALUE.get()).orElse(-1) == (stage == 2 ? 73 : 17), "附件同步阶段 " + stage + " 持有者 " + holder.getClass());
            }
            if (stage == 4) return true;
            int current = stage;
            operation = server.submit(() -> {
                var world = server.overworld();
                List<IAttachmentHolder> targets = List.of(server.getPlayerList().getPlayers().getFirst(), world,
                    world.getChunkAt(POSITION), world.getBlockEntity(POSITION));
                for (IAttachmentHolder target : targets) {
                    if (current == 2) target.removeData(VALUE.get());
                    else target.getData(VALUE.get());
                }
            });
        }
        stage++;
        return false;
    }
}
