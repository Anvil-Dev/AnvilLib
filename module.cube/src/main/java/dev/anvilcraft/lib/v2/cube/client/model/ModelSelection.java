package dev.anvilcraft.lib.v2.cube.client.model;

import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

import java.util.List;

public interface ModelSelection {
    void collect(RandomSource random, List<SelectionPart> output);
    AABB bounds();

    record Fixed(SelectionPart part) implements ModelSelection {
        @Override
        public void collect(RandomSource random, List<SelectionPart> output) { output.add(this.part); }
        @Override
        public AABB bounds() { return this.part.bounds(); }
    }

    record Weighted(List<ModelSelection> variants, List<Integer> weights, int totalWeight, AABB bounds) implements ModelSelection {
        public Weighted { variants = List.copyOf(variants); weights = List.copyOf(weights); }
        @Override
        public void collect(RandomSource random, List<SelectionPart> output) {
            int value = random.nextInt(this.totalWeight);
            if (value < 0) return;
            for (int i = 0; i < this.variants.size(); i++) {
                value -= this.weights.get(i);
                if (value < 0) { this.variants.get(i).collect(random, output); return; }
            }
        }
    }

    record Multipart(List<ModelSelection> parts, AABB bounds) implements ModelSelection {
        public Multipart { parts = List.copyOf(parts); }
        @Override
        public void collect(RandomSource random, List<SelectionPart> output) {
            long seed = random.nextLong();
            for (ModelSelection part : this.parts) part.collect(RandomSource.create(seed), output);
        }
    }
}
