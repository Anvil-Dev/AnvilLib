package dev.anvilcraft.lib.registrar;

import net.minecraft.core.Registry;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class AbstractRegistrar {
    private final String modid;

    protected AbstractRegistrar(String modid) {
        this.modid = modid;
    }

    public ResourceLocation of(String path) {
        return new ResourceLocation(modid, path);
    }

    public <T> TagKeyEntry<T> tag(ResourceKey<? extends Registry<T>> registry, @NotNull String path) {
        return TagKeyEntry.create(this, registry, path, null);
    }

    public <T> TagKeyEntry<T> tag(ResourceKey<? extends Registry<T>> registry, @NotNull String fabricPath, String forgePath) {
        return TagKeyEntry.create(this, registry, fabricPath, forgePath);
    }

    public <T> TagKeyEntry<T> dict(ResourceKey<? extends Registry<T>> registry, @NotNull String path) {
        return TagKeyEntry.create(null, registry, path, null);
    }

    public <T> TagKeyEntry<T> dict(ResourceKey<? extends Registry<T>> registry, @NotNull String fabricPath, String forgePath) {
        return TagKeyEntry.create(null, registry, fabricPath, forgePath);
    }

    public <T extends Item> ItemBuilder<T> item(String name, Function<Item.Properties, T> factory) {
        return new ItemBuilder<>(this, name, factory);
    }

    public <P extends DataProvider> void data(DataProviderType<P> type, Consumer<P> consumer) {
    }

    public void init(){}
}
