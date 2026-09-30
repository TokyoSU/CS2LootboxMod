package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.world.item.Rarity;

import java.util.Locale;

/** CS/CS2 grades with matching Forge Minecraft rarities and exact RGB colors. */
public enum LootRarityGrade {
    CONSUMER_GRADE("consumer", "cs2lootbox.rarity.consumer", CS2LootboxRarity.CONSUMER_COLOR, "common", Rarity.COMMON, CS2LootboxRarity.CONSUMER_GRADE),
    INDUSTRIAL_GRADE("industrial", "cs2lootbox.rarity.industrial", CS2LootboxRarity.INDUSTRIAL_COLOR, "uncommon", Rarity.UNCOMMON, CS2LootboxRarity.INDUSTRIAL_GRADE),
    MIL_SPEC("milspec", "cs2lootbox.rarity.milspec", CS2LootboxRarity.MIL_SPEC_COLOR, "rare", Rarity.RARE, CS2LootboxRarity.MIL_SPEC),
    RESTRICTED("restricted", "cs2lootbox.rarity.restricted", CS2LootboxRarity.RESTRICTED_COLOR, "mythical", Rarity.EPIC, CS2LootboxRarity.RESTRICTED),
    CLASSIFIED("classified", "cs2lootbox.rarity.classified", CS2LootboxRarity.CLASSIFIED_COLOR, "legendary", Rarity.EPIC, CS2LootboxRarity.CLASSIFIED),
    COVERT("covert", "cs2lootbox.rarity.covert", CS2LootboxRarity.COVERT_COLOR, "ancient", Rarity.EPIC, CS2LootboxRarity.COVERT),
    SPECIAL("special", "cs2lootbox.rarity.special", CS2LootboxRarity.SPECIAL_COLOR, "ancient", Rarity.EPIC, CS2LootboxRarity.SPECIAL),
    KNIVES("knives", "cs2lootbox.rarity.knives", CS2LootboxRarity.KNIVES_COLOR, "ancient", Rarity.EPIC, CS2LootboxRarity.KNIVES),
    CONTRABAND("contraband", "cs2lootbox.rarity.contraband", CS2LootboxRarity.CONTRABAND_COLOR, "ancient", Rarity.EPIC, CS2LootboxRarity.CONTRABAND);

    private final String id;
    private final String translationKey;
    private final int color;
    private final String soundBucket;
    private final Rarity vanillaFallback;
    private final Rarity minecraftRarity;

    LootRarityGrade(String id, String translationKey, int color, String soundBucket, Rarity vanillaFallback, Rarity minecraftRarity) {
        this.id = id;
        this.translationKey = translationKey;
        this.color = color;
        this.soundBucket = soundBucket;
        this.vanillaFallback = vanillaFallback;
        this.minecraftRarity = minecraftRarity;
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

    /** Patch labels share the weapon grades' tier IDs and colors. */
    public String patchTranslationKey() {
        return switch (this) {
            case MIL_SPEC -> "cs2lootbox.rarity.patch.high_grade";
            case RESTRICTED -> "cs2lootbox.rarity.patch.remarkable";
            case CLASSIFIED -> "cs2lootbox.rarity.patch.exotic";
            default -> throw new IllegalArgumentException("Unsupported patch grade: " + this);
        };
    }

    public String soundBucket() {
        return soundBucket;
    }

    /** The shared Minecraft rarity used when creating items with this grade. */
    public Rarity minecraftRarity() {
        return minecraftRarity;
    }

    /** Closest vanilla grade for integrations that only support vanilla rarities. */
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
            case "milspec", "mil_spec", "mil_spec_grade", "blue", "rare", "high_grade" -> MIL_SPEC;
            case "restricted", "purple", "mythical", "remarkable" -> RESTRICTED;
            case "classified", "pink", "legendary", "exotic" -> CLASSIFIED;
            case "covert", "red", "ancient" -> COVERT;
            case "knife", "knives" -> KNIVES;
            case "special", "gold", "glove", "gloves" -> SPECIAL;
            case "contraband", "orange" -> CONTRABAND;
            default -> throw new IllegalArgumentException("Unknown loot rarity grade: " + value);
        };
    }
}
