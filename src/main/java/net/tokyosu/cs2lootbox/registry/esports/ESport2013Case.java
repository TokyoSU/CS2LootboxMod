package net.tokyosu.cs2lootbox.registry.esports;

import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;
import net.tokyosu.cs2lootbox.registry.DefaultLootBoxRegistries;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import net.tokyosu.cs2lootbox.registry.LootRarityWeights;
import net.tokyosu.cs2lootbox.registry.ClassicKnifePool;

public class ESport2013Case {
    public static final ResourceLocation CASE_ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "crate_esports_2013");
    
    public static synchronized void register() {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID);

        DefaultLootBoxRegistries.registerNewESportCase(builder, "crate_esports_2013", "crate_esports_2013_key", "crate_esports_2013.png", "weapon_case_key_special_1", 228, 176);
        builder.collection("collection.esports_2013", CS2LootBoxMod.MOD_ID + ":textures/gui/collections/set_esports_2013.png");
        builder.collectionImageScale(1.5F);

        builder.clearLoot().clearLegendary();

        // Equal odds within each grade; normal weights total 99.74. The knife
        // roll happens first, at an independent absolute chance of 0.26%.
        addSkin(builder, "minecraft:trident", "famas.doomkitty", LootRarityGrade.MIL_SPEC);
        addSkin(builder, "minecraft:netherite_sword", "m4a4.faded_zebra", LootRarityGrade.MIL_SPEC);
        addSkin(builder, "minecraft:stone_axe", "mag7.memento", LootRarityGrade.MIL_SPEC);
        addSkin(builder, "minecraft:iron_sword", "galil_ar.orange_ddpat", LootRarityGrade.RESTRICTED);
        addSkin(builder, "minecraft:spyglass", "p250.splash", LootRarityGrade.RESTRICTED);
        addSkin(builder, "minecraft:wooden_axe", "sawedoff.orange_ddpat", LootRarityGrade.RESTRICTED);
        addSkin(builder, "minecraft:diamond_sword", "ak47.red_laminate", LootRarityGrade.CLASSIFIED);
        addSkin(builder, "minecraft:bow", "awp.boom", LootRarityGrade.CLASSIFIED);
        addSkin(builder, "minecraft:crossbow", "p90.death_by_kitty", LootRarityGrade.COVERT);

        ClassicKnifePool.addTo(builder.legendary(0.26D)
                .name("legendary.cs2lootbox.esports_2013.knife")
                .tooltip("tooltip.cs2lootbox.esports_2013.knife")
                .subCarousel(false).subLoot());

        LootboxRegistrationService.register(builder);
    }

    private static void addSkin(@NotNull LootboxDefinitionBuilder builder, @NotNull String item, @NotNull String skin, @NotNull LootRarityGrade tier) {
        // Vanilla items are temporary carriers for the named weapon skins.
        builder.loot(item, LootRarityWeights.perEntry(LootRarityWeights.Profile.ESPORTS_2013, tier), loot -> {
            loot.count(1)
            .name("item.cs2lootbox." + skin)
            .rarityGrade(tier);
            loot.modifiers.add("modifier.stat_track", 10.0D);
        });
    }
}
