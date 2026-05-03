package dev.anvilcraft.lib.v2.recipe.predicate.function;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.ISerializer;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;
import javax.annotation.Nullable;

public interface IPredicateFunction<T> extends BiFunction<InWorldRecipeContext, T, T> {
    Codec<IPredicateFunction<?>> CODEC = LibRegistries.PREDICATE_FUNCTION_TYPE_REGISTRY.byNameCodec()
        .dispatch(IPredicateFunction::getType, Type::codec);

    Type<?> getType();

    interface Type<O extends IPredicateFunction<?>> extends ISerializer<O> {
        default @Nullable Identifier getId() {
            return LibRegistries.PREDICATE_FUNCTION_TYPE_REGISTRY.getKey(this);
        }
    }
}
