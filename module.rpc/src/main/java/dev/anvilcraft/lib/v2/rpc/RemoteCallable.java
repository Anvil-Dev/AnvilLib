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
    /**
     * 接收端校验器：在方法执行前判定本次调用是否被允许。
     *
     * <p>默认值 {@link IRemoteCallableValidator} 本身为「始终放行」哨兵；指定其它实现类时，
     * 该类须有无参构造器，框架会反射实例化并缓存。校验不通过时，{@link RPC#call} 静默丢弃，
     * {@link RPC#invoke} 使调用方 future 以异常失败。</p>
     *
     * @return 校验器类型
     */
    Class<? extends IRemoteCallableValidator> validator() default IRemoteCallableValidator.class;
}
