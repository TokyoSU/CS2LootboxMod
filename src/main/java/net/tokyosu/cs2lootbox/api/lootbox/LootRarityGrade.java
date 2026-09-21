package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.world.item.Rarity;

import java.util.Locale;

/**
 * Built-in CS/CS2 loot grades used by the crate UI.
 *
 * This is deliberately separate from Minecraft's four-value Rarity enum. It
 * gives the lootbox system the real CS grade names/colors today, while leaving
 * a clean place for a later RarityJS bridge to map these ids to custom item
 * rarities without changing crate definitions.
 */
public enum LootRarityGrade {
    CONSUMER_GRADE("consumer", "cs2lootbox.rarity.consumer", 0xB0C3D9, "common", Rarity.COMMON),
    INDUSTRIAL_GRADE("industrial", "cs2lootbox.rarity.industrial", 0x5E98D9, "uncommon", Rarity.UNCOMMON),
    MIL_SPEC("milspec", "cs2lootbox.rarity.milspec", 0x4B69FF, "rare", Rarity.RARE),
    RESTRICTED("restricted", "cs2lootbox.rarity.restricted", 0x8847FF, "mythical", Rarity.EPIC),
    CLASSIFIED("classified", "cs2lootbox.rarity.classified", 0xD32EE6, "legendary", Rarity.EPIC),
    COVERT("covert", "cs2lootbox.rarity.covert", 0xEB4B4B, "ancient", Rarity.EPIC),
    KNIVES("knives", "cs2lootbox.rarity.knives", 0xEB4B4B, "ancient", Rarity.EPIC),
    CONTRABAND("contraband", "cs2lootbox.rarity.contraband", 0xFFAE39, "ancient", Rarity.EPIC);

    private final String id;
    private final String translationKey;
    private final int color;
    private final String soundBucket;
    private final Rarity vanillaFallback;

    LootRarityGrade(String id, String translationKey, int color, String soundBucket, Rarity vanillaFallback) {
        this.id = id;
        this.translationKey = translationKey;
        this.color = color;
        this.soundBucket = soundBucket;
        this.vanillaFallback = vanillaFallback;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public int color() {
        return color;
    }

    public String soundBucket() {
        return soundBucket;
    }

    /** Useful as a fallback until/if a RarityJS bridge supplies custom rarities. */
    public Rarity vanillaFallback() {
        return vanillaFallback;
    }

    public static LootRarityGrade fromId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Loot rarity grade cannot be empty");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "consumer", "consumer_grade", "common", "white", "gray", "grey" -> CONSUMER_GRADE;
            case "industrial", "industrial_grade", "uncommon", "light_blue", "lightblue" -> INDUSTRIAL_GRADE;
            case "milspec", "mil_spec", "mil_spec_grade", "blue", "rare" -> MIL_SPEC;
            case "restricted", "purple", "mythical" -> RESTRICTED;
            case "classified", "pink", "legendary" -> CLASSIFIED;
            case "covert", "red", "ancient" -> COVERT;
            case "knife", "knives", "special", "gold", "glove", "gloves" -> KNIVES;
            case "contraband", "orange" -> CONTRABAND;
            default -> throw new IllegalArgumentException("Unknown loot rarity grade: " + value);
        };
    }
}
