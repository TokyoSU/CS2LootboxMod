package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.world.item.Rarity;

/** Global palette and Minecraft rarities, created once and shared by all items. */
public final class CS2LootboxRarity {
    public static final int CONSUMER_COLOR = 0xB0C3D9;
    public static final int INDUSTRIAL_COLOR = 0x5E98D9;
    public static final int MIL_SPEC_COLOR = 0x4B69FF;
    public static final int RESTRICTED_COLOR = 0x8847FF;
    public static final int CLASSIFIED_COLOR = 0xD32EE6;
    public static final int COVERT_COLOR = 0xEB4B4B;
    public static final int SPECIAL_COLOR = 0xFFAE39;
    public static final int KNIVES_COLOR = COVERT_COLOR;
    public static final int CONTRABAND_COLOR = SPECIAL_COLOR;

    public static final Rarity CONSUMER_GRADE = create("consumer", CONSUMER_COLOR);
    public static final Rarity INDUSTRIAL_GRADE = create("industrial", INDUSTRIAL_COLOR);
    public static final Rarity MIL_SPEC = create("milspec", MIL_SPEC_COLOR);
    public static final Rarity RESTRICTED = create("restricted", RESTRICTED_COLOR);
    public static final Rarity CLASSIFIED = create("classified", CLASSIFIED_COLOR);
    public static final Rarity COVERT = create("covert", COVERT_COLOR);
    public static final Rarity SPECIAL = create("special", SPECIAL_COLOR);
    public static final Rarity KNIVES = create("knives", KNIVES_COLOR);
    public static final Rarity CONTRABAND = create("contraband", CONTRABAND_COLOR);

    private CS2LootboxRarity() {}

    private static Rarity create(String id, int color) {
        return Rarity.create("cs2lootbox:" + id, style -> style.withColor(color));
    }
}
