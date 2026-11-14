package dev.anvilcraft.lib.v2.rendering.preview.gui;

import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** 允许模组为不使用原版菜单注册机制的方块提供只读预览界面。 */
public final class StructureGuiProviders {
    private static final Map<Identifier, Provider> PROVIDERS = new HashMap<>();

    private StructureGuiProviders() {
    }

    /** 在客户端初始化阶段注册；每种方块只能注册一个提供器。 */
    public static void register(Identifier block, Provider provider) {
        Objects.requireNonNull(block);
        Objects.requireNonNull(provider);
        if (PROVIDERS.putIfAbsent(block, provider) != null) {
            throw new IllegalStateException("Structure GUI provider already registered for " + block);
        }
    }

    static @Nullable Provider get(Identifier block) {
        return PROVIDERS.get(block);
    }

    /**
     * 提供器必须使用上下文中的隔离玩家和沙盒数据，不得安装当前屏幕、发包或访问真实库存。
     * 返回的界面仅接收初始化和绘制，不会接收槽位交互、更新或关闭回调。
     */
    @FunctionalInterface
    public interface Provider {
        @Nullable AbstractContainerScreen<?> create(Context context);
    }

    /** 原始方块实体数据可通过 level.getStructureBlockEntityData(pos) 读取。 */
    public record Context(SandboxRenderLevel level, Player player, BlockPos pos, StructureTemplate template) {
    }
}
