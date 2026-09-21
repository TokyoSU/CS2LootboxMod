package net.tokyosu.cs2lootbox.api.lootbox;

import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** Builder for one automatically-appended legendary case panel. */
@SuppressWarnings("unused")
public final class LegendaryLootBuilder {
    private final ResourceLocation ownerId;
    private ResourceLocation foreground = LegendaryLoot.DEFAULT_FOREGROUND;
    private double weight;
    private String tooltipText;
    private final LegendarySubLootBuilder subLoot;

    LegendaryLootBuilder(@NotNull ResourceLocation ownerId, double weight) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.subLoot = new LegendarySubLootBuilder(ownerId);
        weight(weight);
    }

    LegendaryLootBuilder(@NotNull ResourceLocation ownerId, @NotNull LegendaryLoot existing) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        LegendaryLoot source = Objects.requireNonNull(existing, "existing");
        this.foreground = source.foreground();
        this.weight = source.weight();
        this.tooltipText = source.tooltipText();
        this.subLoot = new LegendarySubLootBuilder(ownerId, source.subLoot());
    }

    @Info("Sets this legendary panel's absolute first-carousel chance from 0 to 100 percent. It is independent from the normal loot weights and from subLoot().")
    public @NotNull LegendaryLootBuilder weight(double weight) {
        if (!Double.isFinite(weight) || weight < 0.0D || weight > 100.0D) {
            throw new IllegalArgumentException("Legendary weight must be between 0 and 100: " + weight);
        }
        this.weight = weight;
        return this;
    }

    @Info("Overrides the rare-item icon rendered over the built-in gold legendary panel. If omitted, the mod's default_rare_item.png is used.")
    public @NotNull LegendaryLootBuilder foreground(@NotNull String texture) {
        foreground = parse(texture);
        return this;
    }

    @Info("Sets the gold tooltip line for this legendary panel. Accepts either literal text "
            + "(for example '★ Rare Gloves ★') or a translation key "
            + "(for example tooltip.kubejs.rare_gloves). Each legendary() panel may use a different text.")
    public @NotNull LegendaryLootBuilder tooltip(@NotNull String textOrTranslationKey) {
        if (textOrTranslationKey == null || textOrTranslationKey.isBlank()) {
            throw new IllegalArgumentException("Legendary tooltip text cannot be empty");
        }
        this.tooltipText = textOrTranslationKey.trim();
        return this;
    }

    /** More explicit alias for {@link #tooltip(String)}. */
    public @NotNull LegendaryLootBuilder tooltipText(@NotNull String textOrTranslationKey) {
        return tooltip(textOrTranslationKey);
    }

    @Info("Returns the second-stage weighted loot table displayed after this legendary panel is selected. Its weights are normalized only inside this legendary table.")
    public @NotNull LegendarySubLootBuilder subLoot() {
        return subLoot;
    }

    @NotNull LegendaryLoot build() {
        return new LegendaryLoot(foreground, weight, tooltipText, subLoot.build());
    }

    private @NotNull ResourceLocation parse(@NotNull String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Legendary foreground cannot be empty");
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
}
