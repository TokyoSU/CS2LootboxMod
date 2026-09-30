package net.tokyosu.cs2lootbox.loot;

import net.minecraft.util.RandomSource;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Relative-weight selection; absolute legendary occurrence remains in LootboxLootRoller. */
final class WeightedSelection {
    private WeightedSelection() {}

    static <T> int pick(List<T> values, RandomSource random, ToDoubleFunction<T> weight, double total) {
        if (total <= 0.0D) return -1;
        double value = random.nextDouble() * total;
        double cursor = 0.0D;
        int lastValid = -1;
        for (int i = 0; i < values.size(); i++) {
            double current = weight.applyAsDouble(values.get(i));
            if (current <= 0.0D) continue;
            lastValid = i;
            cursor += current;
            if (value < cursor) return i;
        }
        return lastValid;
    }
}
