/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateGenericProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class RegistrumGenericProvider implements RegistrumProvider {
    private final AbstractRegistrum<?> registrum;
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final LogicalSide side;
    private final ProviderType<RegistrumGenericProvider> providerType;
    private final List<Generator> generators = Lists.newArrayList();

    @ApiStatus.Internal
    RegistrumGenericProvider(
        AbstractRegistrum<?> registrum,
        GatherDataEvent event,
        LogicalSide side,
        ProviderType<RegistrumGenericProvider> providerType
    ) {
        this.registrum = registrum;
        this.side = side;
        this.providerType = providerType;

        output = event.getGenerator().getPackOutput();
        registries = event.getLookupProvider();
    }

    public RegistrumGenericProvider add(Generator generator) {
        generators.add(generator);
        return this;
    }

    @Override
    public LogicalSide getSide() {
        return side;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        generators.clear();
        var data = new GeneratorData(output, registries);
        registrum.genData(providerType, this);
        return CompletableFuture.allOf(generators
            .stream()
            .map(generator -> generator.generate(data))
            .map(provider -> provider.run(cache))
            .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    public String getName() {
        return "generic_%s_provider".formatted(side.name().toLowerCase(Locale.ROOT));
    }

    public record GeneratorData(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    }

    @FunctionalInterface
    public interface Generator {
        DataProvider generate(GeneratorData data);
    }
}
