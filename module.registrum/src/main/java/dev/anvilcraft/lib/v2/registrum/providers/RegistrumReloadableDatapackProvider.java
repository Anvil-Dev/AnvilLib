package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.resources.RegistryDataLoader;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** 为可重新加载的动态注册表生成数据，并向后续生成器提供完整查询。 */
public class RegistrumReloadableDatapackProvider extends DatapackBuiltinEntriesProvider implements RegistrumLookupFillerProvider {
    public RegistrumReloadableDatapackProvider(
        AbstractRegistrum<?> parent,
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> worldRegistries,
        CompletableFuture<HolderLookup.Provider> reloadableRegistries
    ) {
        super(
            output,
            "Registrum reloadable registries",
            RegistryDataLoader.RELOADABLE_REGISTRIES,
            RegistryPatchGenerator.createReloadableLookup(
                worldRegistries, reloadableRegistries, parent.getDataGenInitializer().getReloadableDatapackRegistryProviders()
            ),
            Set.of(parent.getModid())
        );
    }

    @Override
    public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
        return getRegistryProvider();
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }
}
