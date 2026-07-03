package dev.anvilcraft.lib.v2.rendering.integration.mixins;

import com.google.common.collect.ImmutableMap;
import dev.anvilcraft.lib.v2.rendering.integration.IrisSupport;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ALRIntegrationCompatMixinPlugin implements IMixinConfigPlugin {

    private ImmutableMap<String, Boolean> mixinConditions;

    @Override
    public void onLoad(String mixinPackage) {
        mixinConditions = ImmutableMap.<String, Boolean>builder()
            .put(
                "dev.anvilcraft.lib.v2.rendering.integration.mixins.CachedBlockEntityRenderingPipelineMixin",
                isPresent("iris")
            )
            .build();
    }

    private boolean isPresent(String modid) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(modid) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        Boolean bl = mixinConditions.get(mixinClassName);
        System.out.println("mixinClassName = " + mixinClassName);
        return bl == null || bl;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
