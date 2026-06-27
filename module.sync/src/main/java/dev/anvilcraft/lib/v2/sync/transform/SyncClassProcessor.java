package dev.anvilcraft.lib.v2.sync.transform;

import lombok.extern.slf4j.Slf4j;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

@Slf4j
public class SyncClassProcessor implements ClassProcessor {
    @Override
    public ProcessorName name() {
        return ProcessorName.parse("anvillib_sync:processor");
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        String internalName = context.type().getInternalName();
        return SyncTargetIndex.contains(internalName) || LazySyncTargetIndex.contains(internalName);
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        Type type = context.type();
        ClassNode node = context.node();

        boolean modified = false;
        if (SyncTargetIndex.contains(type.getInternalName())) {
            modified |= SyncBytecodeInjector.inject(node);
        }
        if (LazySyncTargetIndex.contains(type.getInternalName())) {
            modified |= LazySyncBytecodeInjector.inject(node);
        }
        if (modified) {
            log.debug("Injected sync calls into {}", type.getInternalName());
            return ComputeFlags.COMPUTE_FRAMES;
        }
        return ComputeFlags.NO_REWRITE;
    }
}
