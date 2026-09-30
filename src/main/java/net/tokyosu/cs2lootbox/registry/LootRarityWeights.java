package net.tokyosu.cs2lootbox.registry;

import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;

/** Equal per-item weights within each built-in container's rarity tiers. */
public final class LootRarityWeights {
    private LootRarityWeights() {}

    public enum Profile {
        WEAPON_CASE(3, 3, 2, 1),
        ESPORTS_2013(3, 3, 2, 1),
        PATCH_PACK(9, 8, 4, 0);

        private final int blue;
        private final int purple;
        private final int pink;
        private final int red;

        Profile(int blue, int purple, int pink, int red) {
            this.blue = blue;
            this.purple = purple;
            this.pink = pink;
            this.red = red;
        }
    }

    /**
     * Normal loot weights only. Weapon special panels retain their separate
     * absolute 0.26% chance. Rounded patch totals are normalized by the roller.
     */
    public static double perEntry(Profile profile, String tier) {
        return perEntry(profile, LootRarityGrade.fromId(tier));
    }

    public static double perEntry(Profile profile, LootRarityGrade tier) {
        boolean patch = profile == Profile.PATCH_PACK;
        double total = switch (tier) {
            case MIL_SPEC -> patch ? 80.65D : 79.92D;
            case RESTRICTED -> patch ? 16.13D : 15.98D;
            case CLASSIFIED -> patch ? 3.23D : 3.20D;
            case COVERT -> 0.64D;
            default -> throw new IllegalArgumentException("Unknown loot rarity tier: " + tier);
        };
        int count = switch (tier) {
            case MIL_SPEC -> profile.blue;
            case RESTRICTED -> profile.purple;
            case CLASSIFIED -> profile.pink;
            case COVERT -> profile.red;
            default -> throw new IllegalArgumentException("Unknown loot rarity tier: " + tier);
        };
        if (count <= 0) {
            throw new IllegalArgumentException("No " + tier + " entries in " + profile);
        }
        return total / count;
    }
}
