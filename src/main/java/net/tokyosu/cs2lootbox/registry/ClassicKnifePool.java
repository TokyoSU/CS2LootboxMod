package net.tokyosu.cs2lootbox.registry;

import net.tokyosu.cs2lootbox.api.lootbox.LegendarySubLootBuilder;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;

/** The 65 original knife variants shared by the Arms Deal and eSports cases. */
public final class ClassicKnifePool {
    private ClassicKnifePool() {}

    public static void addTo(LegendarySubLootBuilder knives) {
        String[] models = {"bayonet", "flip_knife", "gut_knife", "karambit", "m9_bayonet"};
        String[] finishes = {"vanilla", "blue_steel", "boreal_forest", "case_hardened",
                "crimson_web", "fade", "forest_ddpat", "night", "safari_mesh",
                "scorched", "slaughter", "stained", "urban_masked"};
        // Equal weights within the 0.26% special pool. Swords are temporary
        // carriers until dedicated knife items and textures are available.
        for (String model : models) {
            for (String finish : finishes) {
                String knife = model + "." + finish;
                knives.add("minecraft:iron_sword", 1.0D, loot -> {
                    loot.count(1).name("item.cs2lootbox.knife." + knife)
                            .rarityGrade(LootRarityGrade.SPECIAL);
                    loot.modifiers.add("modifier.stat_track", 10.0D);
                });
            }
        }
    }
}
