package net.tokyosu.cs2lootbox.registry;

import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import org.jetbrains.annotations.NotNull;

public class DefaultLootBoxRegistries {
    public static synchronized void registerNewCrate(@NotNull LootboxDefinitionBuilder builder,
                                                     @NotNull String caseRegName,
                                                     @NotNull String caseKeyRegName,
                                                     @NotNull String modelPath,
                                                     @NotNull String texturePath,
                                                     @NotNull String animationPath,
                                                     @NotNull String keyTexture, boolean isCrateWithLock, boolean hasOpenIdleAnim) {
        builder.caseItem("cs2lootbox:" + caseRegName)
                .keyItem("cs2lootbox:" + caseKeyRegName)
                .itemJson("cs2lootbox:item/lootbox_case")
                .model("cs2lootbox:geo/" + modelPath)
                .texture("cs2lootbox:textures/lootbox/" + texturePath)
                .animation("cs2lootbox:animations/" + animationPath)
                .keyTexture("cs2lootbox:item/" + keyTexture)
                .itemIdleAnimation("idle")

                // -----------------------------------------------------------------
                // Mil-Spec / blue — 7 slots
                // -----------------------------------------------------------------
                .loot("minecraft:crossbow", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.famas_meow_36")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:bow", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.galil_destroyer")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:trident", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.m4a4_poly_mag")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:iron_sword", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.mac10_monkeyflage")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:netherite_axe", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.negev_drop_me")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:stone_sword", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.ump45_roadblock")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                .loot("minecraft:golden_sword", BuiltInLootBoxes.milSpecWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.glock18_winterized")
                        .rarity("cs2lootbox.rarity.milspec", 0x4B69FF))

                // -----------------------------------------------------------------
                // Restricted / purple — 5 slots
                // -----------------------------------------------------------------
                .loot("minecraft:flint_and_steel", BuiltInLootBoxes.restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.r8_crazy_8")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:diamond_axe", BuiltInLootBoxes.restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.m249_downtown")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:iron_axe", BuiltInLootBoxes.restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.sg553_dragon_tech")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:stone_axe", BuiltInLootBoxes.restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.p90_vent_rush")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                .loot("minecraft:shears", BuiltInLootBoxes.restrictedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.dual_berettas_flora_carnivora")
                        .rarity("cs2lootbox.rarity.restricted", 0x8847FF))

                // -----------------------------------------------------------------
                // Classified / pink — 3 slots
                // -----------------------------------------------------------------
                .loot("minecraft:diamond_sword", BuiltInLootBoxes.classifiedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.ak47_ice_coaled")
                        .rarity("cs2lootbox.rarity.classified", 0xD32EE6))

                .loot("minecraft:spyglass", BuiltInLootBoxes.classifiedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.p250_visions")
                        .rarity("cs2lootbox.rarity.classified", 0xD32EE6))

                .loot("minecraft:wooden_axe", BuiltInLootBoxes.classifiedWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.sawedoff_kiss_love")
                        .rarity("cs2lootbox.rarity.classified", 0xD32EE6))

                // -----------------------------------------------------------------
                // Covert / red — 2 slots
                // -----------------------------------------------------------------
                .loot("minecraft:netherite_sword", BuiltInLootBoxes.covertWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.usps_printstream")
                        .rarity("cs2lootbox.rarity.covert", 0xEB4B4B))

                .loot("minecraft:enchanted_book", BuiltInLootBoxes.covertWeight, loot -> loot
                        .count(1)
                        .name("loot.cs2lootbox.awp_chromatic_aberration")
                        .rarity("cs2lootbox.rarity.covert", 0xEB4B4B).nbt("{StoredEnchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}"));

        if (hasOpenIdleAnim)
            builder.animations("fall", "idle", "open", "open_idle");
        else
            builder.animations("fall", "idle", "open");

        if (isCrateWithLock)
            builder.openSound("cs2lootbox:case_unlock", 0.2F, 1.0F);
        else
            builder.openSound("cs2lootbox:case_unlock_immediate_01", 0.2F, 1.0F);

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

    }
}
