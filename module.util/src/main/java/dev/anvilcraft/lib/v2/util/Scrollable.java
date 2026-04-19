package dev.anvilcraft.lib.v2.util;

import lombok.Getter;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Range;

@Getter
public abstract class Scrollable {
    private boolean scrolling = false;
    private float scrollOffs = 0.0f;

    public int calculateRowCount() {
        return Mth.positiveCeilDiv(this.size(), this.column()) - this.row();
    }

    public int getRowIndex() {
        return Math.max((int) ((double) (this.scrollOffs * (float) this.calculateRowCount()) + 0.5), 0);
    }

    public void calculateScroll(int rowIndex) {
        this.scrollOffs = Mth.clamp(MathUtil.safeDiv(rowIndex, this.calculateRowCount()), 0.0F, 1.0F);
    }

    public void subtractInputFromScroll(double input) {
        this.scrollOffs = Mth.clamp(this.scrollOffs - (float) MathUtil.safeDiv(input, this.calculateRowCount()), 0.0F, 1.0F);
    }

    public void scrollTo() {
        this.setHead(this.getRowIndex() * this.column());
    }

    public void scrollOnDrag(float barHeight, double mouseY, int top, int bottom) {
        this.scrollOffs = (float) ((mouseY - top - barHeight / 2) / (bottom - top - barHeight));
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
        this.scrollTo();
    }

    public void scrollOnScroll(double scrollY) {
        this.subtractInputFromScroll(scrollY);
        this.scrollTo();
    }

    public void scrolling() {
        this.scrolling = this.canScroll();
    }

    public void notScrolling() {
        this.scrolling = false;
    }

    public void reset() {
        this.scrollOffs = 0.0f;
        this.scrollTo();
    }

    public abstract @Range(from = 0, to = Integer.MAX_VALUE) int row();

    public abstract @Range(from = 0, to = Integer.MAX_VALUE) int column();

    public abstract @Range(from = 0, to = Integer.MAX_VALUE) int size();

    public boolean canScroll() {
        return this.size() > this.row() * this.column();
    }

    public abstract void setHead(int head);
}