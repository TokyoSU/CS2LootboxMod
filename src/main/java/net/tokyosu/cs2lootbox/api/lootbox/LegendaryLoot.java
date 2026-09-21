package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One special/legendary panel shown after all normal case contents.
 *
 * {@code weight} is an absolute 0..100 percent chance for this panel in the
 * first-stage legendary roll. It is NOT added to the normal loot-table total.
 * The entries in {@link #subLoot()} are a separate second-stage weighted table.
 */
public record LegendaryLoot(
        @NotNull ResourceLocation foreground,
        double weight,
        @Nullable String tooltipText,
        @NotNull List<LootEntry> subLoot) {

    public static final @NotNull ResourceLocation DEFAULT_FOREGROUND =
            ResourceLocation.fromNamespaceAndPath(
                    CS2LootBoxMod.MOD_ID,
                    "textures/gui/default_rare_item.png"
            );

    public LegendaryLoot {
        foreground = Objects.requireNonNull(foreground, "foreground");
        if (!Double.isFinite(weight) || weight < 0.0D || weight > 100.0D) {
            throw new IllegalArgumentException("Legendary weight must be between 0 and 100: " + weight);
        }
        subLoot = List.copyOf(Objects.requireNonNull(subLoot, "subLoot"));
    }
}
