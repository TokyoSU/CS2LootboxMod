package net.tokyosu.cs2lootbox.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-authoritative gameplay options.
 * Forge SERVER configs are world-specific and are stored in the world's
 * serverconfig directory.
 */
public final class CS2LootboxServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ALLOW_CASE_OPENING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STATTRACK_ROLLS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STATTRACK_KILL_COUNTING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gameplay");

        ALLOW_CASE_OPENING = builder
                .comment(
                        "Allow players to open lootboxes.",
                        "When disabled, opening requests are rejected server-side.")
                .define("allowCaseOpening", true);

        ENABLE_STATTRACK_ROLLS = builder
                .comment(
                        "Allow modifier.stat_track to be rolled on newly awarded items.",
                        "Existing StatTrak items are not modified by changing this option.")
                .define("enableStatTrackRolls", true);

        ENABLE_STATTRACK_KILL_COUNTING = builder
                .comment(
                        "Allow existing StatTrak items to increment their kill counter.",
                        "This is independent from enableStatTrackRolls.")
                .define("enableStatTrackKillCounting", true);

        builder.pop();
        SPEC = builder.build();
    }
}
