package net.tokyosu.cs2lootbox.api.lootbox;

/**
 * Configuration for the optional StatTrak-style loot modifier.
 *
 * All chances are absolute percentages in the inclusive range 0..100.
 * The star rolls are sequential: two-star is only rolled after one-star
 * succeeded.
 */
public record StatTrackModifier(
        double chance,
        double oneStarChance,
        double twoStarChance) {

    public StatTrackModifier {
        validateChance(chance, "StatTrack chance");
        validateChance(oneStarChance, "StatTrack one-star chance");
        validateChance(twoStarChance, "StatTrack two-star chance");
    }

    private static void validateChance(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
            throw new IllegalArgumentException(name + " must be between 0 and 100: " + value);
        }
    }
}
