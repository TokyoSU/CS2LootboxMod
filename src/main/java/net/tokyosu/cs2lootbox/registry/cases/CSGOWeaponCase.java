package net.tokyosu.cs2lootbox.registry.cases;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;
import net.tokyosu.cs2lootbox.registry.DefaultLootBoxRegistries;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import net.tokyosu.cs2lootbox.registry.LootRarityWeights;
import net.tokyosu.cs2lootbox.registry.ClassicKnifePool;

public final class CSGOWeaponCase {
    public static final ResourceLocation CASE_ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "csgo_case_weapon");

    public static synchronized void register() {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID);

        DefaultLootBoxRegistries.registerNewCrate(builder, "csgo_case_weapon", "csgo_case_weapon_key", "csgo_drop_crate_armsdeal1.geo.json", "csgo_drop_crate_armsdeal1.png", "csgo_drop_crate_armsdeal1.animation.json", "weapon_case_key", true, false);
        builder.collection("collection.arms_deal", CS2LootBoxMod.MOD_ID + ":textures/gui/collections/arms_deal.png");
        builder.collectionImageScale(1.5F);

        // Arms Deal skins, from blue through red.
        addSkin(builder, "minecraft:iron_axe", "item.cs2lootbox.aug.wings", LootRarityGrade.MIL_SPEC);
        addSkin(builder, "minecraft:stone_sword", "item.cs2lootbox.mp7.skulls", LootRarityGrade.MIL_SPEC);
        addSkin(builder, "minecraft:crossbow", "item.cs2lootbox.sg553.ultraviolet", LootRarityGrade.MIL_SPEC);
        addSkin(builder, "minecraft:spyglass", "item.cs2lootbox.glock18.dragon_tattoo", LootRarityGrade.RESTRICTED);
        addSkin(builder, "minecraft:trident", "item.cs2lootbox.m4a1s.dark_water", LootRarityGrade.RESTRICTED);
        addSkin(builder, "minecraft:shears", "item.cs2lootbox.usps.dark_water", LootRarityGrade.RESTRICTED);
        addSkin(builder, "minecraft:diamond_sword", "item.cs2lootbox.ak47.case_hardened", LootRarityGrade.CLASSIFIED);
        addSkin(builder, "minecraft:flint_and_steel", "item.cs2lootbox.desert_eagle.hypnotic", LootRarityGrade.CLASSIFIED);
        addSkin(builder, "minecraft:bow", "item.cs2lootbox.awp.lightning_strike", LootRarityGrade.COVERT);
        
        ClassicKnifePool.addTo(builder.legendary(0.26D)
                .name("legendary.cs2lootbox.arms_deal.knives")
                .tooltip("tooltip.cs2lootbox.arms_deal.knives")
                .subCarousel(false).subLoot());

        LootboxRegistrationService.register(builder);
    }


    private static void addSkin(@NotNull LootboxDefinitionBuilder builder, @NotNull String item, @NotNull String name, @NotNull LootRarityGrade tier) {
        addSkin(builder, item, name, tier, null);
    }

    private static void addSkin(@NotNull LootboxDefinitionBuilder builder, @NotNull String item, @NotNull String name, @NotNull LootRarityGrade tier, @Nullable String nbt) {
        builder.loot(item, LootRarityWeights.perEntry(LootRarityWeights.Profile.WEAPON_CASE, tier), loot -> {
            loot.count(1).name(name).rarityGrade(tier);
            if (nbt != null) {
                loot.nbt(nbt);
            }
            loot.modifiers.add("modifier.stat_track", 10.0D);
        });
    }
}
