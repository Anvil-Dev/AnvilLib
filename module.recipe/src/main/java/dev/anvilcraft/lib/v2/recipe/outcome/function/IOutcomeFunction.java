package dev.anvilcraft.lib.v2.recipe.outcome.function;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.ISerializer;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;
import javax.annotation.Nullable;

public interface IOutcomeFunction<T> extends BiFunction<InWorldRecipeContext, T, T> {
    Codec<IOutcomeFunction<?>> CODEC = LibRegistries.OUTCOM_FUNCTIONE_TYPE_REGISTRY.byNameCodec()
        .dispatch(IOutcomeFunction::getType, Type::codec);

    Type<?> getType();

    interface Type<O extends IOutcomeFunction<?>> extends ISerializer<O> {
        /**
         * 获取配方结果类型的ID
         *
         * @return ID
         */
        default @Nullable Identifier getId() {
            return LibRegistries.OUTCOM_FUNCTIONE_TYPE_REGISTRY.getKey(this);
        }
    }
}
