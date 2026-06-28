package dev.anvilcraft.lib.v2.sync.transform;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 为含 {@link dev.anvilcraft.lib.v2.sync.annotation.LazySync} 字段的类注入跟踪登记调用：
 * <ul>
 *   <li>含实例 {@code @LazySync} 字段 → 在每个 {@code <init>} 末尾注入
 *       {@code AnvilLibSync.LAZY_SYNC_MANAGER.track(this)}；</li>
 *   <li>含静态 {@code @LazySync} 字段 → 在 {@code <clinit>}（不存在则创建）末尾注入
 *       {@code AnvilLibSync.LAZY_SYNC_MANAGER.trackStatic(Owner.class)}。</li>
 * </ul>
 *
 * <p>登记后由 {@link dev.anvilcraft.lib.v2.sync.management.LazySyncManager}
 * 每 tick 扫描差分，故此处无需注入任何字段写入拦截。</p>
 */
@Slf4j
public class LazySyncBytecodeInjector {
    private static final String LAZY_SYNC_DESC = "Ldev/anvilcraft/lib/v2/sync/annotation/LazySync;";
    private static final String ANVIL_LIB_SYNC_INTERNAL = "dev/anvilcraft/lib/v2/sync/AnvilLibSync";
    private static final String MANAGER_FIELD_NAME = "LAZY_SYNC_MANAGER";
    private static final String MANAGER_DESC = "Ldev/anvilcraft/lib/v2/sync/management/LazySyncManager;";
    private static final String MANAGER_INTERNAL = "dev/anvilcraft/lib/v2/sync/management/LazySyncManager";
    private static final String TRACK_NAME = "track";
    private static final String TRACK_DESC = "(Ljava/lang/Object;)V";
    private static final String TRACK_STATIC_NAME = "trackStatic";
    private static final String TRACK_STATIC_DESC = "(Ljava/lang/Class;)V";

    private LazySyncBytecodeInjector() {
    }

    /**
     * 扫描类中带 {@code @LazySync} 的字段并注入对应的跟踪登记调用。
     *
     * @return 是否对类进行了修改
     */
    public static boolean inject(ClassNode classNode) {
        boolean hasInstance = false;
        boolean hasStatic = false;
        for (FieldNode field : classNode.fields) {
            if (!hasLazySync(field)) continue;
            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                hasStatic = true;
            } else {
                hasInstance = true;
            }
        }

        if (!hasInstance && !hasStatic) {
            return false;
        }

        if (hasInstance) {
            for (MethodNode method : classNode.methods) {
                if ("<init>".equals(method.name)) {
                    injectBeforeReturn(method, LazySyncBytecodeInjector::buildInstanceInstructions);
                }
            }
        }
        if (hasStatic) {
            MethodNode clinit = findOrCreateClinit(classNode);
            injectBeforeReturn(clinit, () -> buildStaticInstructions(classNode.name));
        }

        log.debug("Injected LazySync tracking into {} (instance={}, static={})", classNode.name, hasInstance, hasStatic);
        return true;
    }

    private static boolean hasLazySync(FieldNode field) {
        if (field.visibleAnnotations == null) return false;
        for (AnnotationNode annotation : field.visibleAnnotations) {
            if (LAZY_SYNC_DESC.equals(annotation.desc)) return true;
        }
        return false;
    }

    private static MethodNode findOrCreateClinit(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("<clinit>".equals(method.name)) {
                return method;
            }
        }
        MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(clinit);
        return clinit;
    }

    private static void injectBeforeReturn(MethodNode method, Supplier<InsnList> snippet) {
        List<AbstractInsnNode> returns = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                returns.add(insn);
            }
        }
        for (AbstractInsnNode ret : returns) {
            method.instructions.insertBefore(ret, snippet.get());
        }
    }

    /**
     * {@code AnvilLibSync.LAZY_SYNC_MANAGER.track(this);}
     */
    private static InsnList buildInstanceInstructions() {
        InsnList list = new InsnList();
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, ANVIL_LIB_SYNC_INTERNAL, MANAGER_FIELD_NAME, MANAGER_DESC));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MANAGER_INTERNAL, TRACK_NAME, TRACK_DESC, false));
        return list;
    }

    /**
     * {@code AnvilLibSync.LAZY_SYNC_MANAGER.trackStatic(Owner.class);}
     */
    private static InsnList buildStaticInstructions(String owner) {
        InsnList list = new InsnList();
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, ANVIL_LIB_SYNC_INTERNAL, MANAGER_FIELD_NAME, MANAGER_DESC));
        list.add(new LdcInsnNode(Type.getObjectType(owner)));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MANAGER_INTERNAL, TRACK_STATIC_NAME, TRACK_STATIC_DESC, false));
        return list;
    }
}
