package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.lib.v2.sync.annotation.Sync;
import dev.anvilcraft.lib.v2.sync.management.SyncProxy;
import dev.anvilcraft.lib.v2.sync.util.SyncDirection;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.concurrent.CompletableFuture;

/** 在真实服务端 tick 中验证活塞搬运与回拉，另外检查转换器和序列化契约。 */
@EventBusSubscriber(modid = "anvillib_test")
public final class PortRuntimeVerification {
    private static final BlockPos PISTON = new BlockPos(7, -60, 8);
    private static final BlockPos CHEST = PISTON.east();
    private static final BlockPos POWER = PISTON.south();
    private static MinecraftServer server;
    private static ChestBlockEntity chest;
    private static int ticks;
    private static final CompletableFuture<Void> RESULT = new CompletableFuture<>();

    public static CompletableFuture<Void> start(MinecraftServer instance) {
        instance.execute(() -> {
            try {
                var level = instance.overworld();
                level.setBlockAndUpdate(PISTON, Blocks.STICKY_PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
                level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
                chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
                chest.setItem(0, new ItemStack(Items.DIAMOND, 13));
                level.setBlockAndUpdate(POWER, Blocks.REDSTONE_BLOCK.defaultBlockState());
                server = instance;
            } catch (Throwable error) {
                RESULT.completeExceptionally(error);
            }
        });
        return RESULT;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (server == null || RESULT.isDone()) return;
        try {
            var level = server.overworld();
            if (++ticks == 12) {
                checkChest(CHEST.east());
                level.removeBlock(POWER, false);
            } else if (ticks == 24) {
                checkChest(CHEST);
                RESULT.complete(null);
                server = null;
            }
        } catch (Throwable error) {
            RESULT.completeExceptionally(error);
            server = null;
        }
    }

    private static void checkChest(BlockPos position) {
        if (server.overworld().getBlockEntity(position) != chest || !chest.getBlockPos().equals(position)
            || !chest.getItem(0).is(Items.DIAMOND) || chest.getItem(0).getCount() != 13 || chest.isRemoved()) {
            throw new AssertionError("活塞必须保留箱子实例、位置与十三颗钻石: " + position);
        }
    }

    public static void common() throws Exception {
        var registries = net.minecraft.client.Minecraft.getInstance().level.registryAccess();
        var stack = new UnlimitedItemStack(new ItemStack(Items.DIAMOND), 100_000);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var tag = UnlimitedItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        PortVerification.check(UnlimitedItemStack.CODEC.parse(ops, tag).getOrThrow().getCount() == 100_000, "无限堆栈数量往返");
        PortVerification.check(stack.copyWithCount(128).getCount() == 128 && stack.getCount() == 100_000, "无限堆栈副本独立");
        PortVerification.check(new UnlimitedItemStack(new ItemStack(Items.WATER_BUCKET)).getCraftingRemainingItem().is(Items.BUCKET), "合成剩余物品兼容接口");
        PortVerification.check(RegistrumLangProvider.toUpsideDown("a_%1$s").contains("%1$s"), "颠倒英语保留格式占位符");
        PortVerification.check(RegistrumLangProvider.toUpsideDown("_").equals("‾"), "颠倒英语特殊字符映射");
        var fixture = new SyncFixture();
        var parent = SyncProxy.class.getDeclaredField("parent");
        parent.setAccessible(true);
        PortVerification.check(parent.get(fixture.value) == fixture && "value".equals(fixture.value.getFieldName()), "实例同步字段必须经过真实字节码注入");
        PortVerification.check(parent.get(SyncFixture.STATIC) == SyncFixture.class && "STATIC".equals(SyncFixture.STATIC.getFieldName()), "静态同步字段必须经过真实字节码注入");
        PortVerification.check(fixture.value.getDirection() == SyncDirection.S2C, "同步方向注解必须生效");
        var lazy = new LazyFixture();
        var manager = dev.anvilcraft.lib.v2.sync.AnvilLibSync.LAZY_SYNC_MANAGER;
        var clientField = manager.getClass().getDeclaredField("client");
        clientField.setAccessible(true);
        var side = clientField.get(manager);
        var instances = side.getClass().getDeclaredField("instances");
        instances.setAccessible(true);
        PortVerification.check(((java.util.Set<?>) instances.get(side)).contains(lazy), "惰性同步实例必须由构造器自动登记");
        var buffer = io.netty.buffer.Unpooled.buffer();
        try {
            buffer.writeBoolean(false);
            net.minecraft.network.codec.ByteBufCodecs.INT.encode(buffer, 37);
            byte[] encoded = new byte[buffer.readableBytes()];
            buffer.readBytes(encoded);
            manager.applyGrouped(lazy, java.util.List.of(new dev.anvilcraft.lib.v2.sync.management.LazySyncManager.AppliedField(
                LazyFixture.class.getName() + "#value", encoded)));
            PortVerification.check(lazy.value == 37, "惰性同步分组负载写回字段");
        } finally {
            buffer.release();
        }
    }

    @Sync(SyncDirection.S2C)
    public static final class SyncFixture {
        public static final SyncProxy<Integer> STATIC = new SyncProxy<>(1);
        public final SyncProxy<Integer> value = new SyncProxy<>(2);
    }

    public static final class LazyFixture {
        @dev.anvilcraft.lib.v2.sync.annotation.LazySync(SyncDirection.S2C)
        public int value;
    }
}
