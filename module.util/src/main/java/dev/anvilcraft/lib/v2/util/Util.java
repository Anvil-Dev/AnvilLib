package dev.anvilcraft.lib.v2.util;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.SidedThreadGroups;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;

@UtilityClass
public final class Util {
    public static final Direction[][] CORNER_DIRECTIONS = new Direction[][] {
        {Direction.EAST, Direction.NORTH},
        {Direction.EAST, Direction.SOUTH},
        {Direction.WEST, Direction.NORTH},
        {Direction.WEST, Direction.SOUTH},
    };

    /**
     * 判断给定的 {@code modId} 对应的模组是否加载
     *
     * @return 模组是否加载
     */
    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static <E, C extends Collection<E>> Optional<C> intoOptional(C collection) {
        if (collection.isEmpty()) return Optional.empty();
        return Optional.of(collection);
    }

    public static void acceptDirections(BlockPos blockPos, Consumer<BlockPos> blockPosConsumer) {
        for (Direction direction : Direction.values()) {
            blockPosConsumer.accept(blockPos.relative(direction));
        }
        for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            for (Direction vertical : Direction.Plane.VERTICAL) {
                blockPosConsumer.accept(blockPos.relative(horizontal).relative(vertical));
            }
        }
        for (Direction[] corner : CORNER_DIRECTIONS) {
            BlockPos pos1 = blockPos;
            for (Direction direction : corner) {
                pos1 = pos1.relative(direction);
            }
            for (Direction verticalDirection : Direction.Plane.VERTICAL) {
                pos1 = pos1.relative(verticalDirection);
                blockPosConsumer.accept(pos1);
            }
        }
    }

    /**
     * 当前环境是否为客户端
     *
     * @return 是否为客户端
     */
    public static boolean isClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    /**
     * 当前环境是否为服务端
     *
     * @return 是否为服务端
     */
    public static boolean isServer() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
    }

    /**
     * 将传入的值强转为{@code T}类型
     *
     * @param <T> 想要转为的类型
     * @param o   一个值
     * @return 传入的值，但是类型为{@code T}
     * @throws ClassCastException 当无法将传入的值强转时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o) {
        return (T) o;
    }

    /**
     * 若传入的值可被强转为{@code T}类型，则返回包含传入的值的{@link Optional}
     *
     * @param <T> 想要转为的类型
     * @param o   一个值，可为null
     * @return 一个可能包含传入的值的{@link Optional}
     */
    public static <T> Optional<T> castSafely(@Nullable Object o, Class<T> clazz) {
        return Optional.ofNullable(o)
            .filter(clazz::isInstance)
            .map(Util::cast);
    }

    /**
     * 若传入的值可被强转为传入的任意类型，则返回true
     *
     * @param o 一个值，可为null
     * @return 传入的值，但是类型为{@code T}
     */
    @SuppressWarnings("TypeParameterExplicitlyExtendsObject")
    @SafeVarargs
    public static boolean instanceOfAny(@Nullable Object o, Class<? extends Object>... classes) {
        Optional<Object> op = Optional.empty();
        for (Class<?> clazz : classes) {
            op = op.or(() -> Util.castSafely(o, clazz));
        }
        return op.isPresent();
    }

    /**
     * 若传入的值可被强转为{@code T}类型，则使用传入的值执行传入的方法<br>
     * 等效于{@code Util.castSafely(o, clazz).ifPresent(action);}
     *
     * @param <T>    想要转为的类型
     * @param o      一个值，可为null
     * @param action 将要执行的操作
     */
    public static <T> void ifCastable(@Nullable Object o, Class<T> clazz, Consumer<T> action) {
        Optional.ofNullable(o)
            .filter(clazz::isInstance)
            .<T>map(Util::cast)
            .ifPresent(action);
    }

    /**
     * 使用传入的参数运行代码，并返回原参数
     *
     * @param value 原参数
     * @param consumer 需要在传入前调用的方法
     * @param <T> 原参数的类型
     * @return 原参数
     */
    public static <T> T run(T value, Consumer<T> consumer) {
        consumer.accept(value);
        return value;
    }

    /**
     * 抛出一个异常
     *
     * @param throwable 需要抛出的异常
     * @return 无，用于欺骗 IDE
     * @param <T> （并不会）返回的值的类型，用于欺骗 IDE
     * @param <E> 抛出的异常的类型
     * @throws E 抛出的异常的类型
     */
    public static <T, E extends Throwable> T throwE(E throwable) throws E {
        throw throwable;
    }
}
