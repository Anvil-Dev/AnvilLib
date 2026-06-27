package dev.anvilcraft.lib.v2.test.rpc;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于测试 RPC 功能的远程可调用方法集合。
 */
public final class TestRpcMethods {
    @Getter
    private static final List<String> callLog = new CopyOnWriteArrayList<>();
    @Getter
    private static final AtomicInteger invocationCounter = new AtomicInteger(0);

    private TestRpcMethods() {
    }

    public static void clearLog() {
        callLog.clear();
        invocationCounter.set(0);
    }

    // ============ 无参方法 ============

    @RemoteCallable
    public static void noArgs() {
        callLog.add("noArgs");
        invocationCounter.incrementAndGet();
    }

    // ============ 单参方法 - 基础类型 ============

    @RemoteCallable
    public static void withInt(int value) {
        callLog.add("withInt:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withLong(long value) {
        callLog.add("withLong:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withFloat(float value) {
        callLog.add("withFloat:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withDouble(double value) {
        callLog.add("withDouble:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withBoolean(boolean value) {
        callLog.add("withBoolean:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withByte(byte value) {
        callLog.add("withByte:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withShort(short value) {
        callLog.add("withShort:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withString(String value) {
        callLog.add("withString:" + value);
        invocationCounter.incrementAndGet();
    }

    // ============ 包装类型 ============

    @RemoteCallable
    public static void withBoxedInt(Integer value) {
        callLog.add("withBoxedInt:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withBoxedBoolean(Boolean value) {
        callLog.add("withBoxedBoolean:" + value);
        invocationCounter.incrementAndGet();
    }

    // ============ 数组类型 ============

    @RemoteCallable
    public static void withByteArray(byte[] value) {
        callLog.add("withByteArray:" + value.length);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withLongArray(long[] value) {
        callLog.add("withLongArray:" + value.length);
        invocationCounter.incrementAndGet();
    }

    // ============ 多参方法 ============

    @RemoteCallable
    public static void twoParams(String name, int count) {
        callLog.add("twoParams:" + name + "," + count);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void threeParams(int a, String b, boolean c) {
        callLog.add("threeParams:" + a + "," + b + "," + c);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void fourParams(int a, int b, int c, int d) {
        callLog.add("fourParams:" + a + "," + b + "," + c + "," + d);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void fiveParams(int a, int b, int c, int d, int e) {
        callLog.add("fiveParams:" + a + "," + b + "," + c + "," + d + "," + e);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void manyParams(int a, int b, int c, int d, int e, int f, int g, int h) {
        callLog.add("manyParams:" + a + "," + b + "," + c + "," + d + "," + e + "," + f + "," + g + "," + h);
        invocationCounter.incrementAndGet();
    }

    // ============ 自定义编解码器 ============

    @RemoteCallable
    public static void withCompoundTag(CompoundTag tag) {
        callLog.add("withCompoundTag:" + tag.size());
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withCustomCodec(
        @CallableParam(clazz = TestCodecs.class, field = "BLOCK_POS") BlockPos pos
    ) {
        callLog.add("withCustomCodec:" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void withMixedCodecs(
        String name,
        @CallableParam(clazz = TestCodecs.class, field = "BLOCK_POS") BlockPos pos,
        int value
    ) {
        callLog.add("withMixedCodecs:" + name + "," + pos + "," + value);
        invocationCounter.incrementAndGet();
    }

    // ============ 有返回值的方法 ============

    @RemoteCallable
    public static int returnInt() {
        callLog.add("returnInt");
        invocationCounter.incrementAndGet();
        return 42;
    }

    @RemoteCallable
    public static String returnString() {
        callLog.add("returnString");
        invocationCounter.incrementAndGet();
        return "test result";
    }

    @RemoteCallable
    public static boolean returnBoolean() {
        callLog.add("returnBoolean");
        invocationCounter.incrementAndGet();
        return true;
    }

    @RemoteCallable
    public static int computeSum(int a, int b) {
        callLog.add("computeSum:" + a + "," + b);
        invocationCounter.incrementAndGet();
        return a + b;
    }

    @RemoteCallable
    public static String concatenate(String a, String b) {
        callLog.add("concatenate:" + a + "," + b);
        invocationCounter.incrementAndGet();
        return a + b;
    }

    @RemoteCallable
    public static byte[] returnByteArray(int size) {
        callLog.add("returnByteArray:" + size);
        invocationCounter.incrementAndGet();
        return new byte[size];
    }

    @RemoteCallable
    @CallableParam(clazz = TestCodecs.class, field = "BLOCK_POS")
    public static BlockPos returnCustomType(int x, int y, int z) {
        callLog.add("returnCustomType:" + x + "," + y + "," + z);
        invocationCounter.incrementAndGet();
        return new BlockPos(x, y, z);
    }

    // ============ 异常情况 ============

    @RemoteCallable
    public static void throwsException() {
        callLog.add("throwsException");
        invocationCounter.incrementAndGet();
        throw new RuntimeException("Intentional test exception");
    }

    @RemoteCallable
    public static int throwsExceptionWithReturn() {
        callLog.add("throwsExceptionWithReturn");
        invocationCounter.incrementAndGet();
        throw new IllegalArgumentException("Cannot compute result");
    }

    // ============ 带校验器的方法 ============

    @RemoteCallable(validator = AlwaysRejectValidator.class)
    public static void alwaysRejected() {
        callLog.add("alwaysRejected");
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable(validator = ConditionalValidator.class)
    public static void conditionalAccept(int value) {
        callLog.add("conditionalAccept:" + value);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable(validator = ConditionalValidator.class)
    public static int conditionalReturn(int value) {
        callLog.add("conditionalReturn:" + value);
        invocationCounter.incrementAndGet();
        return value * 2;
    }

    // ============ 边界值测试 ============

    @RemoteCallable
    public static void extremeValues(int maxInt, long maxLong, double maxDouble) {
        callLog.add("extremeValues:" + maxInt + "," + maxLong + "," + maxDouble);
        invocationCounter.incrementAndGet();
    }

    @RemoteCallable
    public static void emptyString(String empty) {
        callLog.add("emptyString:'" + empty + "'");
        invocationCounter.incrementAndGet();
    }

    // ============ 校验器实现 ============

    public static class AlwaysRejectValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            return false;
        }
    }

    public static class ConditionalValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            if (args.length == 0) return true;
            if (args[0] instanceof Integer value) {
                return value >= 0;
            }
            return true;
        }
    }

    // ============ 自定义编解码器 ============

    public static class TestCodecs {
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                BlockPos::getX,
                ByteBufCodecs.VAR_INT,
                BlockPos::getY,
                ByteBufCodecs.VAR_INT,
                BlockPos::getZ,
                BlockPos::new
            );
    }
}
