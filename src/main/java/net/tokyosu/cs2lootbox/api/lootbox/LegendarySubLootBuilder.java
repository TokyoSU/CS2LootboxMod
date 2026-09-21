package net.tokyosu.cs2lootbox.api.lootbox;

import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** KubeJS builder for the rewards behind one legendary panel. */
@SuppressWarnings("unused")
public final class LegendarySubLootBuilder {
    private final ResourceLocation ownerId;
    private final List<LootEntry> entries = new ArrayList<>();

    LegendarySubLootBuilder(@NotNull ResourceLocation ownerId) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    }

    LegendarySubLootBuilder(@NotNull ResourceLocation ownerId, @NotNull List<LootEntry> existingEntries) {
        this(ownerId);
        this.entries.addAll(List.copyOf(Objects.requireNonNull(existingEntries, "existingEntries")));
    }

    @Info(value = "Adds one legendary reward. Weight is 0..100 and is normalized only against the other entries in this legendary sub-loot table.", params = {
            @Param(name = "item", value = "Registered item id or #item_tag"),
            @Param(name = "weight", value = "Legendary sub-loot weight from 0 to 100")
    })
    public @NotNull LegendarySubLootBuilder add(@NotNull String item, double weight) {
        entries.add(asLegendary(createLootEntryBuilder(item, weight).build()));
        return this;
    }

    @Info("Adds one legendary reward with a fixed stack count.")
    public @NotNull LegendarySubLootBuilder addCount(@NotNull String item, double weight, int count) {
        LootEntryBuilder builder = createLootEntryBuilder(item, weight);
        builder.count(count);
        entries.add(asLegendary(builder.build()));
        return this;
    }

    @Info("Adds one legendary reward with an inclusive random stack-count range.")
    public @NotNull LegendarySubLootBuilder add(@NotNull String item, double weight, int minCount, int maxCount) {
        LootEntryBuilder builder = createLootEntryBuilder(item, weight);
        builder.count(minCount, maxCount);
        entries.add(asLegendary(builder.build()));
        return this;
    }

    @Info(value = "Adds and configures one legendary reward.", params = {
            @Param(name = "item", value = "Registered item id or #item_tag"),
            @Param(name = "weight", value = "Legendary sub-loot weight from 0 to 100"),
            @Param(name = "config", value = "Reward configuration callback")
    })
    public @NotNull LegendarySubLootBuilder add(@NotNull String item, double weight, @NotNull Consumer<LootEntryBuilder> config) {
        LootEntryBuilder builder = createLootEntryBuilder(item, weight);
        Objects.requireNonNull(config, "config").accept(builder);
        entries.add(asLegendary(builder.build()));
        return this;
    }

    @NotNull List<LootEntry> build() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("Legendary sub-loot cannot be empty");
        }

        double total = 0.0D;
        for (LootEntry entry : entries) {
            if (entry != null && Double.isFinite(entry.weight()) && entry.weight() > 0.0D) {
                total += entry.weight();
            }
        }

        if (total <= 0.0D) {
            throw new IllegalStateException("Legendary sub-loot must contain at least one entry with a positive weight");
        }

        return List.copyOf(entries);
    }

    private @NotNull LootEntryBuilder createLootEntryBuilder(@NotNull String source, double weight) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Legendary sub-loot item/tag cannot be empty");
        }

        String normalized = source.trim();
        boolean tagSource = normalized.startsWith("#");
        if (tagSource) {
            normalized = normalized.substring(1).trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Legendary sub-loot item tag cannot be empty");
            }
        }

        return new LootEntryBuilder(parse(normalized), weight, tagSource);
    }

    private @NotNull ResourceLocation parse(@NotNull String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Legendary sub-loot item id cannot be empty");
        }
        String normalized = value.trim();
        int separator = normalized.indexOf(':');
        if (separator >= 0) {
            return ResourceLocation.fromNamespaceAndPath(
                    normalized.substring(0, separator),
                    normalized.substring(separator + 1)
            );
        }
        return ResourceLocation.fromNamespaceAndPath(ownerId.getNamespace(), normalized);
    }

    private static @NotNull LootEntry asLegendary(@NotNull LootEntry entry) {
        if (entry.rarityTier() != null && !entry.rarityTier().isBlank()) {
            return entry;
        }

        String rarityKey = entry.rarityTranslationKey() != null
                ? entry.rarityTranslationKey()
                : "cs2lootbox.rarity.covert";
        int rarityColor = entry.rarityColor() != -1
                ? entry.rarityColor()
                : 0xEB4B4B;

        return new LootEntry(
                entry.itemId(),
                entry.itemTagSource(),
                entry.weight(),
                entry.minCount(),
                entry.maxCount(),
                entry.nameTranslationKey(),
                rarityKey,
                "covert",
                rarityColor,
                entry.descriptionTranslationKeys(),
                entry.statTrackModifier(),
                entry.itemNbt(),
                entry.itemListTransform()
        );
    }
}
