package dev.anvilcraft.lib.v2.network.register;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;

/**
 * 网络包注册器
 *
 * <p>应在 {@link net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent RegisterPayloadHandlersEvent} 的侦听器中使用</p>
 *
 * @see NetworkRegistrar#register(PayloadRegistrar, String)
 */
@Slf4j
public class NetworkRegistrar {
    private static final String ANNOTATION_NAME = "L" + Network.class.getName().replace(".", "/") + ";";
    private static final String PACKET_PACKAGE_PREFIX = "L" + IPacket.class.getPackageName().replace(".", "/");

    /**
     * 注册对应 {@code modId} 的模组中所有使用 {@link Network} 注解的软件包下的网络包
     *
     * @param registrar 网络包注册器。应通过
     *                  {@link
     *                  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent#registrar(String)
     *                  RegisterPayloadHandlersEvent.registrar()
     *                  } 获取
     * @param modId     模组 ID
     */
    @SuppressWarnings(
        {
            "unchecked",
            "UnstableApiUsage"
        }
    )
    public static void register(PayloadRegistrar registrar, String modId) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        IModFileInfo fileInfo = FMLLoader.getCurrent().getLoadingModList().getModFileById(modId);
        ModFileScanData scanData = fileInfo.getFile().getScanResult();
        for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
            if (
                !annotation.annotationType().getDescriptor().equals(ANNOTATION_NAME)
                || annotation.targetType() != ElementType.TYPE
            ) {
                continue;
            }

            ModAnnotation.EnumHolder protocolHolder = (ModAnnotation.EnumHolder) annotation.annotationData().get("protocol");
            PacketProtocol protocol = PacketProtocol.PLAY;
            if (protocolHolder != null) {
                protocol = switch (protocolHolder.value()) {
                    case "CONFIGURATION" -> PacketProtocol.CONFIGURATION;
                    case "PLAY" -> PacketProtocol.PLAY;
                    case "COMMON" -> PacketProtocol.COMMON;
                    default -> throw new IllegalArgumentException("Unknown packet protocol: " + protocolHolder.value());
                };
            }
            String packageName = annotation.memberName().substring(0, annotation.memberName().lastIndexOf('.'));
            log.info("Considering network package {}", packageName);

            for (ModFileScanData.ClassData classData : scanData.getClasses()) {
                String className = classData.clazz().getClassName();
                if (!className.substring(0, className.lastIndexOf('.')).equals(packageName)) continue;

                boolean isPacket = false;
                for (Type anInterface : classData.interfaces()) {
                    if (!anInterface.getDescriptor().startsWith(NetworkRegistrar.PACKET_PACKAGE_PREFIX)) continue;
                    isPacket = true;
                    break;
                }
                if (!isPacket) continue;

                try {
                    Class<? extends IPacket> packetClass = (Class<? extends IPacket>) loader.loadClass(className);
                    NetworkRegistrar.register(registrar, protocol, packetClass);
                } catch (ClassNotFoundException e) {
                    log.error("Cannot find packet class {}", className, e);
                    throw new IllegalStateException();
                }
            }
        }
    }

    private static <T extends IPacket> void register(PayloadRegistrar registrar, PacketProtocol protocol, Class<T> packetClass) {
        switch (protocol) {
            case CONFIGURATION -> {
                PacketData<? super FriendlyByteBuf, T> data = PacketData.find(packetClass);
                log.debug("Registered packet {} for 'CONFIGURATION', '{}'", data.type().id(), data.direction());
                switch (data.direction()) {
                    case CLIENTBOUND -> registrar.configurationToClient(data.type(), data.streamCodec(), data.handler());
                    case SERVERBOUND -> registrar.configurationToServer(data.type(), data.streamCodec(), data.handler());
                    case BIDIRECTIONAL ->
                        registrar.configurationBidirectional(data.type(), data.streamCodec(), data.handler(), data.handler());
                }
            }
            case PLAY -> {
                PacketData<? super RegistryFriendlyByteBuf, T> data = PacketData.find(packetClass);
                log.debug("Registered packet {} for 'PLAY', '{}'", data.type().id(), data.direction());
                switch (data.direction()) {
                    case CLIENTBOUND -> registrar.playToClient(data.type(), data.streamCodec(), data.handler());
                    case SERVERBOUND -> registrar.playToServer(data.type(), data.streamCodec(), data.handler());
                    case BIDIRECTIONAL -> registrar.playBidirectional(data.type(), data.streamCodec(), data.handler(), data.handler());
                }
            }
            case COMMON -> {
                PacketData<? super FriendlyByteBuf, T> data = PacketData.find(packetClass);
                log.debug("Registered packet {} for 'COMMON', '{}'", data.type().id(), data.direction());
                switch (data.direction()) {
                    case CLIENTBOUND -> registrar.commonToClient(data.type(), data.streamCodec(), data.handler());
                    case SERVERBOUND -> registrar.commonToServer(data.type(), data.streamCodec(), data.handler());
                    case BIDIRECTIONAL -> registrar.commonBidirectional(data.type(), data.streamCodec(), data.handler(), data.handler());
                }
            }
        }
    }
}
