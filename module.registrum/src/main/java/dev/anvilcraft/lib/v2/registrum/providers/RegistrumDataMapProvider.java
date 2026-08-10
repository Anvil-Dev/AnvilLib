/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/providers/RegistrateDataMapProvider.java
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DataMapProvider;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class RegistrumDataMapProvider extends DataMapProvider implements RegistrumProvider {

	private final AbstractRegistrum<?> parent;

	private HolderLookup.@Nullable Provider provider;

	protected RegistrumDataMapProvider(AbstractRegistrum<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> pvd) {
		super(output, pvd);
		this.parent = parent;
	}

	@Override
	public LogicalSide getSide() {
		return LogicalSide.SERVER;
	}

    /**
     * Generate data map entries.
     *
     * @param provider HolderLookup.Provider
     */
    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.provider = provider;
        parent.genData(ProviderType.DATA_MAP, this);
        this.provider = null;
    }

    /**
     * 当前 datagen 运行中可用的 {@link HolderLookup.Provider}。
     *
     * @throws IllegalStateException 若不在 {@link #gather(HolderLookup.Provider)} 调用期间
     */
    public HolderLookup.Provider getProvider() {
        if (provider == null) throw new IllegalStateException("Holder Lookup Provider is not available now");
        return provider;
    }

}
