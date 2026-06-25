package dev.anvilcraft.lib.v2.rpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个静态方法为可远程调用方法。
 *
 * <p>仅被该注解标注的 {@code static} 方法才能作为 {@link RPC#call} 的目标，
 * 接收端在执行前会校验目标方法确实带有此注解，以防止任意方法被远程触发。</p>
 *
 * @see RPC#call
 * @see CallableParam
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteCallable {
}
