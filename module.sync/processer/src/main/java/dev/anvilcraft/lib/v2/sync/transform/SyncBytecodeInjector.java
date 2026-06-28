package dev.anvilcraft.lib.v2.sync.transform;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Injects SyncProxy#setParent(...) calls at the end of <init> and <clinit> methods
 * for all final/static-final SyncProxy fields found in the target class.
 * <p>
 * Instance fields  → proxy.setParent(this)       in every <init>
 * Static fields    → proxy.setParent(Owner.class) in <clinit> (created if absent)
 */
@Slf4j
public class SyncBytecodeInjector {
    private static final String SYNC_PROXY_DESC = "Ldev/anvilcraft/lib/v2/sync/management/SyncProxy;";
    private static final String SYNC_PROXY_INTERNAL = "dev/anvilcraft/lib/v2/sync/management/SyncProxy";
    private static final String SET_PARENT_NAME = "setParent";
    private static final String SET_PARENT_DESC = "(Ljava/lang/Object;)V";
    private static final String SET_FIELD_NAME_NAME = "setFieldName";
    private static final String SET_FIELD_NAME_DESC = "(Ljava/lang/String;)V";
    private static final String SET_DIRECTION_NAME = "setDirection";
    private static final String SET_DIRECTION_DESC = "(Ldev/anvilcraft/lib/v2/sync/util/SyncDirection;)V";
    private static final String SYNC_DIRECTION_INTERNAL = "dev/anvilcraft/lib/v2/sync/util/SyncDirection";
    private static final String SYNC_DIRECTION_DESC = "Ldev/anvilcraft/lib/v2/sync/util/SyncDirection;";

    private SyncBytecodeInjector() {
    }

    /**
     * Entry point. Scans the class for qualifying SyncProxy fields and injects
     * setParent calls in the appropriate constructor/static initializer.
     */
    public static boolean inject(ClassNode classNode) {
        String owner = classNode.name;

        List<FieldNode> instanceFields = new ArrayList<>();
        List<FieldNode> staticFields = new ArrayList<>();

        for (FieldNode field : classNode.fields) {
            if (!field.desc.equals(SYNC_PROXY_DESC)) continue;
            if ((field.access & Opcodes.ACC_FINAL) == 0) {
                log.warn("SyncProxy field {}.{} is not final, skipping", owner, field.name);
                continue;
            }

            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                staticFields.add(field);
            } else {
                instanceFields.add(field);
            }
        }

        if (instanceFields.isEmpty() && staticFields.isEmpty()) {
            log.info("No qualifying SyncProxy fields found in {}, skipping injection", owner);
            return false;
        }

        if (!instanceFields.isEmpty()) {
            injectIntoInit(classNode, owner, instanceFields);
        }
        if (!staticFields.isEmpty()) {
            injectIntoClinit(classNode, owner, staticFields);
        }

        log.debug(
            "Injected setParent into {} ({} instance, {} static field(s))",
            owner,
            instanceFields.size(),
            staticFields.size()
        );
        return true;
    }

    // -----------------------------------------------------------------------
    // Instance constructor injection
    // -----------------------------------------------------------------------

    private static void injectIntoInit(ClassNode classNode, String owner, List<FieldNode> fields) {
        String direction = SyncTargetIndex.getDirection(classNode.name);
        for (MethodNode method : classNode.methods) {
            if ("<init>".equals(method.name)) {
                injectBeforeReturn(method, () -> buildInstanceInstructions(owner, fields, direction));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Static initializer injection
    // -----------------------------------------------------------------------

    private static void injectIntoClinit(ClassNode classNode, String owner, List<FieldNode> fields) {
        String direction = SyncTargetIndex.getDirection(classNode.name);
        MethodNode clinit = findOrCreateClinit(classNode);
        injectBeforeReturn(clinit, () -> buildStaticInstructions(owner, fields, direction));
    }

    private static MethodNode findOrCreateClinit(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if ("<clinit>".equals(method.name)) {
                return method;
            }
        }
        // No <clinit> yet – create a minimal one and append it
        MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(clinit);
        return clinit;
    }

    // -----------------------------------------------------------------------
    // Generic "insert before every RETURN" helper
    // -----------------------------------------------------------------------

    private static void injectBeforeReturn(MethodNode method, Supplier<InsnList> snippet) {
        // Collect RETURN nodes first to avoid ConcurrentModificationException
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

    // -----------------------------------------------------------------------
    // Bytecode snippets
    // -----------------------------------------------------------------------

    /**
     * For each field:
     * <pre>
     * if (this.field != null) this.field.setParent(this);
     * </pre>
     */
    private static InsnList buildInstanceInstructions(String owner, List<FieldNode> fields, String direction) {
        InsnList list = new InsnList();
        String directionName = toDirectionName(direction);
        for (FieldNode field : fields) {
            LabelNode skip = new LabelNode();

            // null check: if (this.field == null) goto skip
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field.name, SYNC_PROXY_DESC));
            list.add(new JumpInsnNode(Opcodes.IFNULL, skip));

            // this.field.setParent(this)
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field.name, SYNC_PROXY_DESC));
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));    // "this" as the parent Object
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SYNC_PROXY_INTERNAL, SET_PARENT_NAME, SET_PARENT_DESC, false));

            // this.field.setFieldName("fieldName")
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field.name, SYNC_PROXY_DESC));
            list.add(new LdcInsnNode(field.name));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SYNC_PROXY_INTERNAL, SET_FIELD_NAME_NAME, SET_FIELD_NAME_DESC, false));

            // this.field.setDirection(direction)
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field.name, SYNC_PROXY_DESC));
            list.add(new FieldInsnNode(Opcodes.GETSTATIC, SYNC_DIRECTION_INTERNAL, directionName, SYNC_DIRECTION_DESC));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SYNC_PROXY_INTERNAL, SET_DIRECTION_NAME, SET_DIRECTION_DESC, false));
            list.add(skip);
        }
        return list;
    }

    /**
     * For each field:
     * <pre>
     * if (Owner.field != null) Owner.field.setParent(Owner.class);
     * </pre>
     */
    private static InsnList buildStaticInstructions(String owner, List<FieldNode> fields, String direction) {
        InsnList list = new InsnList();
        String directionName = toDirectionName(direction);
        for (FieldNode field : fields) {
            LabelNode skip = new LabelNode();

            // null check: if (Owner.field == null) goto skip
            list.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, field.name, SYNC_PROXY_DESC));
            list.add(new JumpInsnNode(Opcodes.IFNULL, skip));

            // Owner.field.setParent(Owner.class)
            list.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, field.name, SYNC_PROXY_DESC));
            list.add(new LdcInsnNode(Type.getObjectType(owner)));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SYNC_PROXY_INTERNAL, SET_PARENT_NAME, SET_PARENT_DESC, false));

            // Owner.field.setFieldName("fieldName")
            list.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, field.name, SYNC_PROXY_DESC));
            list.add(new LdcInsnNode(field.name));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SYNC_PROXY_INTERNAL, SET_FIELD_NAME_NAME, SET_FIELD_NAME_DESC, false));

            // Owner.field.setDirection(direction)
            list.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, field.name, SYNC_PROXY_DESC));
            list.add(new FieldInsnNode(Opcodes.GETSTATIC, SYNC_DIRECTION_INTERNAL, directionName, SYNC_DIRECTION_DESC));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SYNC_PROXY_INTERNAL, SET_DIRECTION_NAME, SET_DIRECTION_DESC, false));

            list.add(skip);
        }
        return list;
    }

    private static String toDirectionName(String direction) {
        if (direction.isBlank()) {
            return "BOTH";
        }
        String value = direction;
        int dot = value.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < value.length()) {
            value = value.substring(dot + 1);
        }
        value = value.toUpperCase();
        if (!"C2S".equals(value) && !"S2C".equals(value) && !"BOTH".equals(value)) {
            return "BOTH";
        }
        return value;
    }
}

