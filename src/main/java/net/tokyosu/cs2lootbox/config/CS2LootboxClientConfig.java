package net.tokyosu.cs2lootbox.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only presentation options.
 *
 * These options never change the authoritative server reward roll.
 */
public final class CS2LootboxClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_CASE_LOOT_TOOLTIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_3D_ITEM_INSPECTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RARITY_GLOWS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_UI_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CAROUSEL_TICK_SOUND;
    public static final ForgeConfigSpec.BooleanValue SHOW_STATTRACK_KILL_COUNT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("tooltips");

        SHOW_CASE_LOOT_TOOLTIP = builder
                .comment(
                        "Show the generated case contents tooltip.",
                        "The tooltip is built from the finalized startup loot table.")
                .define("showCaseLootTooltip", true);

        SHOW_STATTRACK_KILL_COUNT = builder
                .comment(
                        "Show the StatTrak kill counter below an item's display name.",
                        "The StatTrak prefix remains part of the actual item display name.")
                .define("showStatTrackKillCount", true);

        builder.pop();

        builder.push("inspection");

        ENABLE_3D_ITEM_INSPECTION = builder
                .comment(
                        "Allow left-click 3D inspection of normal crate.loot(...) entries.",
                        "legendary(...) panels remain non-inspectable.")
                .define("enable3DItemInspection", true);

        builder.pop();

        builder.push("visuals");

        ENABLE_RARITY_GLOWS = builder
                .comment(
                        "Render CS-style rarity/legendary glow effects in the carousel and prize screen.")
                .define("enableRarityGlows", true);

        builder.pop();

        builder.push("sounds");

        ENABLE_UI_SOUNDS = builder
                .comment(
                        "Enable lootbox UI sounds such as unlock, reveal, awarded and menu sounds.")
                .define("enableUiSounds", true);

        ENABLE_CAROUSEL_TICK_SOUND = builder
                .comment(
                        "Enable the repeated carousel item-scroll tick sound.",
                        "This only has an effect when enableUiSounds is also true.")
                .define("enableCarouselTickSound", true);

        builder.pop();
        SPEC = builder.build();
    }
}
