package dev.anvilcraft.lib.v2.math.init;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * math 库的注册表：函数类型注册表与函数数据包注册表。
 *
 * <p>函数类型注册表的条目决定 {@link IFunction} 的编解码方式；函数数据包注册表的条目则是可以被
 * 表达式按名引用的具体函数。下游模组要在数据包里按 {@link CustomFunction} 的格式提供 JSON 来注册函数——
 * <b>这个注册表不能由代码注册</b>：它是数据包注册表，只在数据包加载时由 JSON 填充，而
 * {@code DeferredRegister} 依赖的 {@code RegisterEvent} 不会为数据包注册表触发。代码里要用函数，直接用
 * {@code Holder.direct(function)} 构造内联定义即可（{@code RegistryFileCodec} 会把它写成
 * {@code {function: {...}}} 并能读回）。</p>
 *
 * <p><b>命名空间约定：</b>模块 id 是 {@code anvillib_math}，但两个注册表的键都挂在
 * {@link AnvilLibMath#MAIN_ID}（{@code anvillib}）下。flat 文本里不带命名空间的名字会补成
 * {@code anvillib}，所以想用短名引用的函数必须注册在 {@code anvillib} 命名空间；注册在别处就得写全名。
 * 其它模块若也往这两个注册表注册，需要自己保证名字不冲突——{@code anvillib} 下的条目是所有模块共用的
 * 一个命名空间。</p>
 */
@EventBusSubscriber(modid = AnvilLibMath.MOD_ID)
public class LibRegistries {
    /**
     * 函数类型注册表，条目为 {@link IFunction.Type}。
     */
    public static final ResourceKey<Registry<IFunction.Type<?>>> FUNCTION_TYPE_KEY = ResourceKey
        .createRegistryKey(AnvilLibMath.of("function_type"));
    public static final Registry<IFunction.Type<?>> FUNCTION_TYPE = new RegistryBuilder<>(FUNCTION_TYPE_KEY)
        .sync(true)
        .maxId(512)
        .create();
    /**
     * 函数数据包注册表，条目为可被引用的 {@link IFunction}。
     */
    public static final ResourceKey<Registry<IFunction>> FUNCTION_KEY = ResourceKey
        .createRegistryKey(AnvilLibMath.of("function"));

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(LibRegistries.FUNCTION_TYPE);
    }

    @SubscribeEvent
    public static void registerDataRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
            LibRegistries.FUNCTION_KEY,
            IFunction.DIRECT_CODEC,
            IFunction.DIRECT_CODEC
        );
    }
}
