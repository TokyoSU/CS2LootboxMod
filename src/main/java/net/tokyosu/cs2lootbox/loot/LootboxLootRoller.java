package net.tokyosu.cs2lootbox.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.tokyosu.cs2lootbox.api.lootbox.LegendaryLoot;
import net.tokyosu.cs2lootbox.api.lootbox.LootEntry;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.api.lootbox.StatTrackModifier;
import net.tokyosu.cs2lootbox.config.CS2LootboxServerConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared weighted-selection logic used by the authoritative server roll and client visuals. */
public final class LootboxLootRoller {
    private LootboxLootRoller() {
    }

    /**
     * Rolls the first carousel and, when it selects a legendary panel, also
     * resolves that panel's second-stage reward immediately on the server.
     */
    public static @Nullable RollResult roll(@NotNull LootboxDefinition definition, @NotNull RandomSource random) {
        PrimaryPick primary = pickPrimary(definition, random);
        if (primary == null) {
            return null;
        }

        if (!primary.legendary()) {
            LootEntry entry = definition.loot().get(primary.index());
            ItemStack stack = createStack(entry, random);
            if (stack.isEmpty()) {
                return null;
            }
            applyRolledModifiers(stack, entry, random);
            return RollResult.normal(primary.index(), stack);
        }

        LegendaryLoot legendary = definition.legendaryLoot().get(primary.index());
        int subLootIndex = pickEntryIndex(legendary.subLoot(), random);
        if (subLootIndex < 0) {
            return null;
        }

        LootEntry entry = legendary.subLoot().get(subLootIndex);
        ItemStack stack = createStack(entry, random);
        if (stack.isEmpty()) {
            return null;
        }
        applyRolledModifiers(stack, entry, random);

        return RollResult.legendary(primary.index(), subLootIndex, stack);
    }

    /**
     * Selects one first-carousel entry. Legendary panel weights are absolute
     * percentages and are evaluated separately from the normal loot pool.
     *
     * Example: legendary(0.26) means a real 0.26% chance regardless of whether
     * the normal loot weights total 10, 100 or 1000. When no legendary panel is
     * hit, a normal item is selected using only the normal loot weights.
     *
     * This method is used for the authoritative reward roll. Client filler slots
     * use the same configured chance as a visual density target, but are spaced
     * separately so cosmetic gold panels do not cluster and cannot affect the roll.
     */
    public static @Nullable PrimaryPick pickPrimary(@NotNull LootboxDefinition definition, @NotNull RandomSource random) {
        int legendaryIndex = pickLegendaryIndex(definition.legendaryLoot(), random);
        if (legendaryIndex >= 0) {
            return PrimaryPick.legendary(legendaryIndex);
        }

        int normalIndex = pickEntryIndex(definition.loot(), random);
        return normalIndex >= 0 ? PrimaryPick.normal(normalIndex) : null;
    }

    /**
     * Performs the authoritative absolute-percentage legendary check.
     *
     * A configured value of {@code 1.0} is exactly a 1% chance per case opening,
     * independent from the normal loot-table weight total. When several legendary
     * panels exist, each panel occupies its own absolute percentage band.
     *
     * @return the selected legendary panel index, or {@code -1} when this roll is normal
     */
    public static int pickLegendaryIndex(@NotNull List<LegendaryLoot> legendaryLoot, @NotNull RandomSource random) {
        double totalChance = totalLegendaryChance(legendaryLoot);
        if (totalChance <= 0.0D) {
            return -1;
        }

        // nextDouble() is uniformly distributed in [0, 1), therefore this is
        // uniformly distributed in [0, 100). A 1.0 chance occupies [0, 1),
        // which is exactly one percent of the interval.
        double roll = random.nextDouble() * 100.0D;
        if (roll >= totalChance) {
            return -1;
        }

        double cursor = 0.0D;
        for (int i = 0; i < legendaryLoot.size(); i++) {
            double chance = validLegendaryWeight(legendaryLoot.get(i));
            if (chance <= 0.0D) {
                continue;
            }

            cursor += chance;
            if (roll < cursor) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Chooses one legendary panel after a visual legendary occurrence is already
     * known to exist. This does not perform another absolute-chance check; it only
     * distributes that occurrence among configured panels according to their
     * relative legendary chances.
     */
    public static int pickLegendaryPanelIndex(@NotNull List<LegendaryLoot> legendaryLoot, @NotNull RandomSource random) {
        double totalChance = totalLegendaryChance(legendaryLoot);
        if (totalChance <= 0.0D) {
            return -1;
        }

        double value = random.nextDouble() * totalChance;
        double cursor = 0.0D;
        int lastValid = -1;

        for (int i = 0; i < legendaryLoot.size(); i++) {
            double chance = validLegendaryWeight(legendaryLoot.get(i));
            if (chance <= 0.0D) {
                continue;
            }

            lastValid = i;
            cursor += chance;
            if (value < cursor) {
                return i;
            }
        }

        return lastValid;
    }

    /** Returns the combined absolute first-stage legendary chance, capped at 100%. */
    public static double totalLegendaryChance(@NotNull List<LegendaryLoot> legendaryLoot) {
        double total = 0.0D;
        for (LegendaryLoot legendary : legendaryLoot) {
            total += validLegendaryWeight(legendary);
        }
        return Math.min(100.0D, total);
    }

    public static int pickEntryIndex(@NotNull List<LootEntry> entries, @NotNull RandomSource random) {
        double total = 0.0D;
        int lastValid = -1;

        for (int i = 0; i < entries.size(); i++) {
            LootEntry entry = entries.get(i);
            if (isValid(entry)) {
                total += entry.weight();
                lastValid = i;
            }
        }

        if (total <= 0.0D || lastValid < 0) {
            return -1;
        }

        double value = random.nextDouble() * total;
        double cursor = 0.0D;

        for (int i = 0; i < entries.size(); i++) {
            LootEntry entry = entries.get(i);
            if (!isValid(entry)) {
                continue;
            }

            cursor += entry.weight();
            if (value < cursor) {
                return i;
            }
        }

        return lastValid;
    }

    public static @NotNull ItemStack createStack(@NotNull LootEntry entry, @NotNull RandomSource random) {
        Item item = resolveRollItem(entry, random);
        if (item == null || entry.weight() <= 0.0D) {
            return ItemStack.EMPTY;
        }

        int min = Math.max(1, entry.minCount());
        int max = Math.max(min, entry.maxCount());
        int count = min == max ? min : min + random.nextInt(max - min + 1);
        return createStack(entry, item, count);
    }

    /**
     * Creates a deterministic preview stack for tooltips, item_list and the 3D
     * inspector. For a #tag source, the first registry-sorted member is used.
     */
    public static @NotNull ItemStack createPreviewStack(@NotNull LootEntry entry) {
        return createPreviewStack(entry, Math.max(1, entry.minCount()));
    }

    public static @NotNull ItemStack createPreviewStack(@NotNull LootEntry entry, int count) {
        Item item = resolvePreviewItem(entry);
        return item == null ? ItemStack.EMPTY : createStack(entry, item, count);
    }

    private static @NotNull ItemStack createStack(
            @NotNull LootEntry entry,
            @NotNull Item item,
            int requestedCount) {
        ItemStack stack = new ItemStack(item);

        CompoundTag configuredNbt = entry.itemNbt();
        if (configuredNbt != null) {
            stack.setTag(configuredNbt);
        }

        int count = Math.min(Math.max(1, requestedCount), stack.getMaxStackSize());
        stack.setCount(count);
        return stack;
    }

    public static @Nullable Item resolvePreviewItem(@NotNull LootEntry entry) {
        if (!entry.itemTagSource()) {
            return ForgeRegistries.ITEMS.getValue(entry.itemId());
        }

        List<Item> items = resolveTagItems(entry);
        return items.isEmpty() ? null : items.get(0);
    }

    private static @Nullable Item resolveRollItem(
            @NotNull LootEntry entry,
            @NotNull RandomSource random) {
        if (!entry.itemTagSource()) {
            return ForgeRegistries.ITEMS.getValue(entry.itemId());
        }

        List<Item> items = resolveTagItems(entry);
        return items.isEmpty() ? null : items.get(random.nextInt(items.size()));
    }

    private static @NotNull List<Item> resolveTagItems(@NotNull LootEntry entry) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, entry.itemId());
        List<Item> items = new ArrayList<>();

        BuiltInRegistries.ITEM.getTag(key).ifPresent(tag ->
                tag.forEach(holder -> items.add(holder.value())));

        items.sort(Comparator.comparing(item -> {
            var id = ForgeRegistries.ITEMS.getKey(item);
            return id == null ? "" : id.toString();
        }));
        return items;
    }


    /**
     * Applies reward mutations only to the authoritative selected reward.
     * Carousel filler stacks call createStack() directly and never perform
     * modifier rolls.
     */
    private static void applyRolledModifiers(
            @NotNull ItemStack stack,
            @NotNull LootEntry entry,
            @NotNull RandomSource random) {
        if (!CS2LootboxServerConfig.ENABLE_STATTRACK_ROLLS.get()) {
            return;
        }

        StatTrackModifier statTrack = entry.statTrackModifier();
        if (statTrack == null || !rollPercent(random, statTrack.chance())) {
            return;
        }

        int stars = 0;
        if (rollPercent(random, statTrack.oneStarChance())) {
            stars = 1;
            if (rollPercent(random, statTrack.twoStarChance())) {
                stars = 2;
            }
        }

        StatTrackUtil.initialize(stack, stars);
    }

    private static boolean rollPercent(@NotNull RandomSource random, double chance) {
        if (!Double.isFinite(chance) || chance <= 0.0D) {
            return false;
        }
        if (chance >= 100.0D) {
            return true;
        }
        return random.nextDouble() * 100.0D < chance;
    }

    public static double validWeight(@NotNull List<LootEntry> entries) {
        double total = 0.0D;
        for (LootEntry entry : entries) {
            if (isValid(entry)) {
                total += entry.weight();
            }
        }
        return total;
    }


    private static double validLegendaryWeight(@Nullable LegendaryLoot legendary) {
        if (legendary == null
                || !Double.isFinite(legendary.weight())
                || legendary.weight() <= 0.0D
                || validWeight(legendary.subLoot()) <= 0.0D) {
            return 0.0D;
        }
        return legendary.weight();
    }

    private static boolean isValid(@Nullable LootEntry entry) {
        return entry != null
                && Double.isFinite(entry.weight())
                && entry.weight() > 0.0D
                && resolvePreviewItem(entry) != null;
    }

    public record PrimaryPick(boolean legendary, int index) {
        public static @NotNull PrimaryPick normal(int entryIndex) {
            return new PrimaryPick(false, entryIndex);
        }

        public static @NotNull PrimaryPick legendary(int legendaryIndex) {
            return new PrimaryPick(true, legendaryIndex);
        }
    }

    public record RollResult(
            boolean legendary,
            int entryIndex,
            int legendaryIndex,
            int legendaryEntryIndex,
            @NotNull ItemStack stack) {

        public static @NotNull RollResult normal(int entryIndex, @NotNull ItemStack stack) {
            return new RollResult(false, entryIndex, -1, -1, stack);
        }

        public static @NotNull RollResult legendary(int legendaryIndex, int legendaryEntryIndex, @NotNull ItemStack stack) {
            return new RollResult(true, -1, legendaryIndex, legendaryEntryIndex, stack);
        }
    }
}
