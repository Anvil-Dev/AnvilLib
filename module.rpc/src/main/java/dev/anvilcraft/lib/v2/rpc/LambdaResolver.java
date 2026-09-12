package dev.anvilcraft.lib.v2.rpc;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * 从可序列化的方法引用中还原出其指向的目标方法。
 *
 * <p>{@link RPC#call} 接收的方法引用（如 {@code SomeClass::someStaticMethod}）会被编译为实现了
 * {@link Serializable} 的 lambda 实例。JVM 为这类实例自动生成 {@code writeReplace} 方法，返回携带实现类、
 * 方法名与方法描述符的 {@link SerializedLambda}，据此即可定位目标方法。</p>
 */
final class LambdaResolver {
    private LambdaResolver() {
    }

    /**
     * 解析方法引用所指向的、经 {@link RemoteCallable} 校验的静态方法。
     *
     * @param methodRef 指向静态方法的可序列化方法引用
     * @return 已校验通过的目标方法
     */
    static Method resolve(Serializable methodRef) {
        SerializedLambda lambda = serializedLambda(methodRef);
        String className = lambda.getImplClass().replace('/', '.');
        return RpcMethods.resolve(className, lambda.getImplMethodName(), lambda.getImplMethodSignature());
    }

    private static SerializedLambda serializedLambda(Serializable methodRef) {
        try {
            Method writeReplace = methodRef.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object replacement = writeReplace.invoke(methodRef);
            if (replacement instanceof SerializedLambda lambda) {
                return lambda;
            }
            throw new IllegalArgumentException(
                "RPC target must be a method reference to a static method, got: " + methodRef.getClass().getName()
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot extract method reference metadata", e);
        }
    }
}
