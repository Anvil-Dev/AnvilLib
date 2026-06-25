package dev.anvilcraft.lib.v2.rpc;

import net.minecraft.network.codec.StreamCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为 {@link RemoteCallable} 方法的某个参数指定 {@link StreamCodec}。
 *
 * <p>当参数类型不在
 * {@link net.minecraft.network.codec.ByteBufCodecs ByteBufCodecs} 默认支持的类型之列时，
 * 需用本注解指向一个 {@code public static final StreamCodec} 字段，用于该参数的网络编解码。</p>
 *
 * <h2>示例</h2>
 * <pre>{@code
 * @RemoteCallable
 * public static void teleport(
 *     @CallableParam(clazz = MyCodecs.class, field = "BLOCK_POS") BlockPos pos
 * ) { ... }
 * }</pre>
 *
 * @see RemoteCallable
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CallableParam {
    /**
     * 声明 {@link StreamCodec} 字段的类。
     *
     * @return 持有编解码器字段的类
     */
    Class<?> clazz();

    /**
     * {@link StreamCodec} 字段的名称。该字段须为 {@code static}。
     *
     * @return 编解码器字段名
     */
    String field();
}
