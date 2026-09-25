package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.expression.FlatExpressionParser;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import net.minecraft.core.HolderGetter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("解析缓存的存活期与容量")
class ParseCacheTest {
    @BeforeAll
    static void setUp() {
        MathTestBootstrap.initialize();
    }

    @Test
    @DisplayName("同一个注册表反复解析同一段文本，拿到的是同一棵树")
    void cacheHitsForTheSameRegistry() {
        // 缓存按注册表分组，键是 HolderGetter 的实例；同一个实例才命中
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> getter =
            MathTestBootstrap.functions();
        FlatExpressionParser.clearCache();
        IExpression first = FlatExpressionParser.parseValue("1+2", getter);
        IExpression second = FlatExpressionParser.parseValue("1+2", getter);
        assertSame(first, second, "同一段文本应当命中缓存、拿到同一个实例");
    }

    @Test
    @DisplayName("clearCache 之后重新解析，拿到的是新树")
    void clearCacheForcesAFreshParse() {
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> getter =
            MathTestBootstrap.functions();
        IExpression before = FlatExpressionParser.parseValue("2+3", getter);
        FlatExpressionParser.clearCache();
        IExpression after = FlatExpressionParser.parseValue("2+3", getter);
        assertNotSame(before, after, "clearCache 之后应当重新解析");
    }

    @Test
    @DisplayName("超过容量上限时最旧的条目被淘汰")
    void cacheEvictsTheOldestBeyondCapacity() {
        // 容量是 512（见 FlatExpressionParser.CACHE_CAPACITY）；LinkedHashMap 的 accessOrder
        // 让「最久没用过的」先走，所以塞满之后再取最早那条会重新解析
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> getter =
            MathTestBootstrap.functions();
        FlatExpressionParser.clearCache();
        IExpression oldest = FlatExpressionParser.parseValue("1", getter);
        // 再塞满 512 条，把最旧的挤出去
        for (int index = 0; index <= 512; index++) {
            FlatExpressionParser.parseValue(Integer.toString(1000 + index), getter);
        }
        IExpression again = FlatExpressionParser.parseValue("1", getter);
        assertNotSame(oldest, again, "超过上限后最早的条目应当已被淘汰");
        // 刚写进去的那条还在
        IExpression recent = FlatExpressionParser.parseValue("1512", getter);
        assertSame(recent, FlatExpressionParser.parseValue("1512", getter), "最近写入的条目应当还在缓存里");
    }

    @Test
    @DisplayName("同一个注册表拿两次 lookup 仍是同一组缓存")
    void lookupsOfTheSameRegistryShareABucket() {
        // 26.1 里注册表自己就是 HolderLookup.RegistryLookup（也就是 HolderGetter），
        // 同一注册表拿两次是同一个实例，所以缓存不会因为「每次新取一个 lookup」而永远命中不了
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> first =
            MathTestBootstrap.functions();
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> second =
            MathTestBootstrap.functions();
        assertSame(first, second, "同一注册表的 lookup 应当是同一个实例");
        FlatExpressionParser.clearCache();
        IExpression fromFirst = FlatExpressionParser.parseValue("3+4", first);
        IExpression fromSecond = FlatExpressionParser.parseValue("3+4", second);
        assertSame(fromFirst, fromSecond, "同一注册表应当共用同一个缓存分组");
    }

    @Test
    @DisplayName("编解码路径每次拿到的 getter 都能命中同一个缓存分组")
    void codecGetterIsStableEnoughToCache() {
        // 缓存是按 getter 实例分组的，所以真正要钉的是「编解码路径反复取 getter 时拿到的是不是同一个」，
        // 否则缓存形同虚设
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> first =
            dev.anvilcraft.lib.v2.math.expression.function.IFunction.getter(MathTestBootstrap.ops());
        HolderGetter<dev.anvilcraft.lib.v2.math.expression.function.IFunction> second =
            dev.anvilcraft.lib.v2.math.expression.function.IFunction.getter(MathTestBootstrap.ops());
        assertSame(first, second, "编解码路径反复取 getter 应当拿到同一个实例");
        assertSame(
            MathTestBootstrap.functions(),
            first,
            "取到的应当就是注册表自己"
        );
    }
}
