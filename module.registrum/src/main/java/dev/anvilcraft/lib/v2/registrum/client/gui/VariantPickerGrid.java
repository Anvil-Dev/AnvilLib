package dev.anvilcraft.lib.v2.registrum.client.gui;

final class VariantPickerGrid {
    static final int COLUMNS = 4;
    static final int ROWS = 4;
    private static final int CAPACITY = COLUMNS * ROWS;

    private final int itemCount;
    private final int maxRow;
    private int row;
    private double scrollRemainder;

    VariantPickerGrid(int itemCount) {
        this.itemCount = itemCount;
        this.maxRow = Math.max(0, (itemCount - 1) / COLUMNS + 1 - ROWS);
    }

    boolean isScrollable() {
        return this.maxRow > 0;
    }

    int firstIndex() {
        return this.row * COLUMNS;
    }

    int visibleCount() {
        return Math.min(CAPACITY, this.itemCount - this.firstIndex());
    }

    int indexAt(int cell) {
        return cell < 0 || cell >= this.visibleCount() ? -1 : this.firstIndex() + cell;
    }

    void scroll(double amount) {
        if (!this.isScrollable() || amount == 0) {
            return;
        }
        if (Math.signum(amount) != Math.signum(this.scrollRemainder)) {
            this.scrollRemainder = 0;
        }
        this.scrollRemainder += amount;
        int steps = (int) this.scrollRemainder;
        this.scrollRemainder -= steps;
        this.row = Math.max(0, Math.min(this.maxRow, this.row - steps));
        if ((this.row == 0 && amount > 0) || (this.row == this.maxRow && amount < 0)) {
            this.scrollRemainder = 0;
        }
    }

    void scrollTo(double fraction) {
        this.row = (int) Math.round(Math.max(0, Math.min(1, fraction)) * this.maxRow);
        this.scrollRemainder = 0;
    }

    double scrollFraction() {
        return this.isScrollable() ? (double) this.row / this.maxRow : 0;
    }
}
