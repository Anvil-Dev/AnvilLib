package dev.anvilcraft.lib.v2.sync.transform;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.neoforgespi.coremod.ICoreMod;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 以 CoreMod（{@link ICoreMod} + {@link ITransformer}<{@link ClassNode}>）方式实现
 * 原 26.1 的 {@code neoforgespi.transformation.ClassProcessor} SPI 字节码注入能力。
 *
 * <p>1.21.1 的 fancymodloader loader 4.0.x 不含 transformation SPI 包，
 * 等价能力为 {@code META-INF/services/net.neoforged.neoforgespi.coremod.ICoreMod}
 * 注册的 CoreMod。注入器（{@link SyncBytecodeInjector} /
 * {@link LazySyncBytecodeInjector}）为纯 ASM {@link ClassNode} 操作，两侧复用。</p>
 *
 * <p>时序说明：{@link SyncTargetIndex} / {@link LazySyncTargetIndex} 的静态扫描
 * 依赖 {@code LoadingModList.get()}（mod 发现完成后就绪）；CoreMod 的
 * {@link #getTransformers()} 在 FMLServiceProvider.launch()（mod 扫描之后）被调用，
 * 故此时索引已填充，{@link SyncTransformer#targets()} 可精确列出目标类。</p>
 */
@Slf4j
@ApiStatus.Internal
public class SyncClassProcessor implements ICoreMod {
    @Override
    public Iterable<? extends ITransformer<?>> getTransformers() {
        return List.of(new SyncTransformer());
    }

    private static class SyncTransformer implements ITransformer<ClassNode> {
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            String internalName = input.name;
            boolean modified = false;
            if (SyncTargetIndex.contains(internalName)) {
                modified |= SyncBytecodeInjector.inject(input);
            }
            if (LazySyncTargetIndex.contains(internalName)) {
                modified |= LazySyncBytecodeInjector.inject(input);
            }
            if (modified) {
                log.debug("Injected sync calls into {}", internalName);
            }
            return input;
        }

        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @Override
        public Set<Target<ClassNode>> targets() {
            Set<Target<ClassNode>> targets = new HashSet<>();
            for (String internalName : SyncTargetIndex.allTargets()) {
                targets.add(Target.targetClass(internalName.replace('/', '.')));
            }
            for (String internalName : LazySyncTargetIndex.allTargets()) {
                targets.add(Target.targetClass(internalName.replace('/', '.')));
            }
            return targets;
        }

        @Override
        public TargetType<ClassNode> getTargetType() {
            return TargetType.CLASS;
        }
    }
}
