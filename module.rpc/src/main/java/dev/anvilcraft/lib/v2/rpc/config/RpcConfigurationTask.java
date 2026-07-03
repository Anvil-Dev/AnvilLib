package dev.anvilcraft.lib.v2.rpc.config;

import dev.anvilcraft.lib.v2.rpc.AnvilLibRpc;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * 服务端配置任务：在 Configuration 阶段向客户端下发权威的 RPC 索引映射。
 */
public record RpcConfigurationTask() implements ICustomConfigurationTask {
    public static final Type TYPE = new Type(AnvilLibRpc.mod("rpc_config"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new RpcConfigurationPayload(AnvilLibRpc.REGISTRY.snapshot()));
    }

    @Override
    public Type type() {
        return RpcConfigurationTask.TYPE;
    }
}
