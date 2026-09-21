package net.tokyosu.cs2lootbox.registry;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;

/**
 * Built-in CS:GO Weapon Case content.
 *
 * The weapon/skin slots currently use distinct vanilla Minecraft items as
 * placeholders until the actual weapon registry exists. Their visible names,
 * rarity colors and case ordering already match the configured CS-style table.
 */
public final class BuiltInLootboxes {
    public static final ResourceLocation CASE_ID =
            ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "csgo_case_weapon");

    /** Backwards-compatible alias used by existing UI/default lookup code. */
    public static final ResourceLocation DEFAULT_ID = CASE_ID;

    private static boolean registered;

    private BuiltInLootboxes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        /*
         * Overall first-stage rarity targets:
         *
         *  Mil-Spec   : 79.92 %
         *  Restricted : 15.98 %
         *  Classified :  3.20 %
         *  Covert     :  0.64 %
         *  Legendary  :  0.26 %
         *
         * legendary(0.26) is an absolute server-side percentage. The normal
         * entries below deliberately total 99.74, so after the 0.26% legendary
         * check the conditional normal roll still produces the exact overall
         * category percentages above.
         */
        final double milSpecWeight = 79.92D / 7.0D;
        final double restrictedWeight = 0.98D / 5.0D;
        final double classifiedWeight = 3.20D / 3.0D;
        final double covertWeight = 15.64D / 2.0D;

        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID)
                .caseItem("cs2lootbox:csgo_case_weapon")
                .keyItem("cs2lootbox:csgo_case_weapon_key")
                .itemJson("cs2lootbox:item/lootbox_case")
                .model("cs2lootbox:geo/csgo_drop_crate_armsdeal1.geo.json")
                .texture("cs2lootbox:textures/lootbox/csgo_drop_crate_armsdeal1.png")
                .animation("cs2lootbox:animations/csgo_drop_crate_armsdeal1.animation.json")
                .keyTexture("cs2lootbox:item/weapon_case_key")
                .animations("fall", "idle", "open")
                .itemIdleAnimation("idle")
                .collection(
                        "collection.arms_deal",
                        CS2LootBoxMod.MOD_ID + ":textures/gui/collections/arms_deal.png"
                )
                .caseName("item.cs2lootbox.csgo_case_weapon")
                .keyName("item.cs2lootbox.csgo_case_weapon_key")
                .openSound("cs2lootbox:case_unlock", 0.2F, 1.0F)

                // -----------------------------------------------------------------
                // Mil-Spec / blue — 7 slots
                // -----------------------------------------------------------------
                .loot("minecraft:crossbow", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.famas_meow_36")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:bow", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.galil_destroyer")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:trident", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.m4a4_poly_mag")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:iron_sword", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.mac10_monkeyflage")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:netherite_axe", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.negev_drop_me")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:stone_sword", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.ump45_roadblock")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:golden_sword", milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.glock18_winterized")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                // -----------------------------------------------------------------
                // Restricted / purple — 5 slots
                // -----------------------------------------------------------------
                .loot("minecraft:flint_and_steel", restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.r8_crazy_8")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:diamond_axe", restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.m249_downtown")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:iron_axe", restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.sg553_dragon_tech")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:stone_axe", restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.p90_vent_rush")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:shears", restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.dual_berettas_flora_carnivora")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                // -----------------------------------------------------------------
                // Classified / pink — 3 slots
                // -----------------------------------------------------------------
                .loot("minecraft:diamond_sword", classifiedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.ak47_ice_coaled")
                        .rarity("cs2lootbox.rarity.classified", 0xD32EE6))

                .loot("minecraft:spyglass", classifiedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.p250_visions")
                        .rarity("cs2lootbox.rarity.classified", 0xD32EE6))

                .loot("minecraft:wooden_axe", classifiedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.sawedoff_kiss_love")
                        .rarity("cs2lootbox.rarity.classified", 0xD32EE6))

                // -----------------------------------------------------------------
                // Covert / red — 2 slots
                // -----------------------------------------------------------------
                .loot("minecraft:netherite_sword", covertWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.usps_printstream")
                        .rarity("cs2lootbox.rarity.covert", 0xEB4B4B))

                .loot("minecraft:enchanted_book", covertWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.awp_chromatic_aberration")
                        .rarity("cs2lootbox.rarity.covert", 0xEB4B4B).nbt("{StoredEnchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}"));

        // ---------------------------------------------------------------------
        // Legendary / gold — absolute 0.26%
        // ---------------------------------------------------------------------
        builder.legendary(20.0D)
                .name("legendary.kubejs.rare_gloves")
                .tooltip("tooltip.kubejs.rare_gloves")
                .subCarousel(false)
                .subLoot()
                // Temporary glove placeholder. Replace this with the real
                // glove/knife item ids when those registries are available.
                .add("minecraft:leather_boots", 60.0D, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.gloves")
                        .rarity("cs2lootbox.rarity.special", 0xFFAE39))
                .add("minecraft:apple", 40.0D, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.apple")
                        .rarity("cs2lootbox.rarity.special", 0xFFAE39));

        LootboxRegistrationService.register(builder);
    }
}
