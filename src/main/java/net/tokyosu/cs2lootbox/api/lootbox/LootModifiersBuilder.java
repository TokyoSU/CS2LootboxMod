package net.tokyosu.cs2lootbox.api.lootbox;

import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** KubeJS-facing modifier collection for one loot entry. */
@SuppressWarnings("unused")
public final class LootModifiersBuilder {
    private StatTrackModifier statTrack;

    @Info(value = "Adds a loot modifier. modifier.stat_track rolls when the authoritative server reward is created.", params = {
            @Param(name = "modifier", value = "Modifier id, currently modifier.stat_track"),
            @Param(name = "chance", value = "Absolute percentage chance from 0 to 100")
    })
    public @NotNull LootModifiersBuilder add(@NotNull String modifier, double chance) {
        return add(modifier, chance, 0.0D, 0.0D);
    }

    @Info(value = "Adds a StatTrack modifier and optional one-star mutation chance.", params = {
            @Param(name = "modifier", value = "Modifier id, currently modifier.stat_track"),
            @Param(name = "chance", value = "StatTrack chance from 0 to 100"),
            @Param(name = "oneStarChance", value = "Conditional one-star chance from 0 to 100")
    })
    public @NotNull LootModifiersBuilder add(@NotNull String modifier, double chance, double oneStarChance) {
        return add(modifier, chance, oneStarChance, 0.0D);
    }

    @Info(value = "Adds a StatTrack modifier. The two-star roll only occurs if the one-star roll succeeded.", params = {
            @Param(name = "modifier", value = "Modifier id, currently modifier.stat_track"),
            @Param(name = "chance", value = "StatTrack chance from 0 to 100"),
            @Param(name = "oneStarChance", value = "Conditional one-star chance from 0 to 100"),
            @Param(name = "twoStarChance", value = "Conditional two-star chance from 0 to 100 after one-star succeeds")
    })
    public @NotNull LootModifiersBuilder add(
            @NotNull String modifier,
            double chance,
            double oneStarChance,
            double twoStarChance) {
        String normalized = normalizeModifier(modifier);
        if (!normalized.equals("modifier.stat_track")) {
            throw new IllegalArgumentException("Unknown loot modifier: " + modifier);
        }

        statTrack = new StatTrackModifier(chance, oneStarChance, twoStarChance);
        return this;
    }

    @Nullable StatTrackModifier statTrack() {
        return statTrack;
    }

    private static @NotNull String normalizeModifier(@NotNull String modifier) {
        if (modifier == null || modifier.isBlank()) {
            throw new IllegalArgumentException("Loot modifier id cannot be empty");
        }

        String normalized = modifier.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("stat_track") || normalized.equals("stattrack") || normalized.equals("cs2lootbox:stat_track")) {
            return "modifier.stat_track";
        }
        return normalized;
    }
}
