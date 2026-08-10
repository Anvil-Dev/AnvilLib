package dev.anvilcraft.lib.v2.rendering.mixins;

import dev.anvilcraft.lib.v2.rendering.extension.GuiGraphicsExtractorExtension;
import dev.anvilcraft.lib.v2.rendering.internal.ItemStackRenderStateInternals;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(GuiGraphicsExtractor.class)
@ApiStatus.Internal
public class GuiGraphicsExtractorMixin implements GuiGraphicsExtractorExtension {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Shadow
    @Final
    private GuiGraphicsExtractor.ScissorStack scissorStack;

    @Override
    public void translucentItem(@Nullable LivingEntity owner, @Nullable Level level, ItemStack stack, int x, int y, float alpha, int seed) {
        if (!stack.isEmpty()) {
            TrackingItemStackRenderState itemStackRenderState = new TrackingItemStackRenderState();
            this.minecraft.getItemModelResolver().updateForTopItem(itemStackRenderState, stack, ItemDisplayContext.GUI, level, owner, seed);
            ItemStackRenderStateInternals.setAlpha(itemStackRenderState, alpha);
            try {
                this.guiRenderState.addItem(
                    new GuiItemRenderState(new Matrix3x2f(this.pose),
                        itemStackRenderState,
                        x,
                        y,
                        this.scissorStack.peek()
                    )
                );
            } catch (Throwable t) {
                CrashReport report = CrashReport.forThrowable(t, "Rendering item");
                CrashReportCategory category = report.addCategory("Item being rendered");
                category.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
                category.setDetail("Item Components", () -> String.valueOf(stack.getComponents()));
                category.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
                throw new ReportedException(report);
            }
        }
    }
}
