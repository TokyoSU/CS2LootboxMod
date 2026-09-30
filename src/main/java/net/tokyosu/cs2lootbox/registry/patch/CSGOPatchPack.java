package net.tokyosu.cs2lootbox.registry.patch;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;
import net.tokyosu.cs2lootbox.registry.DefaultLootBoxRegistries;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import net.tokyosu.cs2lootbox.registry.LootRarityWeights;

public class CSGOPatchPack {
    public static final ResourceLocation CASE_ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "csgo_patch_pack");

    public static synchronized void register() {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID);

        DefaultLootBoxRegistries.registerNewPatch(builder, "csgo_patch_pack", "patch_envelope.geo.json", "patch_envelope.png", "patch_envelope.animation.json");
        builder.tooltipDescription("item.cs2lootbox.csgo_patch_pack.desc");

        // Replace the shared helper's demo loot with this pack's 21 patches.
        builder.clearLoot().clearLegendary();

        // 9 high grade patches (milspec).
        addPatch(builder, "shattered_web", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "hydra", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "wildfire", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "bloodhound", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "vanguard", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "breakout", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "phoenix", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "bravo", LootRarityGrade.MIL_SPEC);
        addPatch(builder, "payback", LootRarityGrade.MIL_SPEC);

        // 8 remarkable patches (restricted).
        addPatch(builder, "vigilance", LootRarityGrade.RESTRICTED);
        addPatch(builder, "longevity", LootRarityGrade.RESTRICTED);
        addPatch(builder, "koi", LootRarityGrade.RESTRICTED);
        addPatch(builder, "welcome_to_the_clutch", LootRarityGrade.RESTRICTED);
        addPatch(builder, "crazy_banana", LootRarityGrade.RESTRICTED);
        addPatch(builder, "chicken_lover", LootRarityGrade.RESTRICTED);
        addPatch(builder, "easy_peasy", LootRarityGrade.RESTRICTED);
        addPatch(builder, "danger_zone", LootRarityGrade.RESTRICTED);

        // 4 exotic patches (classified).
        addPatch(builder, "howl", LootRarityGrade.CLASSIFIED);
        addPatch(builder, "the_boss", LootRarityGrade.CLASSIFIED);
        addPatch(builder, "dragon", LootRarityGrade.CLASSIFIED);
        addPatch(builder, "rage", LootRarityGrade.CLASSIFIED);

        LootboxRegistrationService.register(builder);
    }

    private static void addPatch(LootboxDefinitionBuilder builder, String patch, LootRarityGrade tier) {
        String textureName = switch (patch) {
            case "shattered_web" -> "shatteredweb";
            case "welcome_to_the_clutch" -> "clutch";
            case "crazy_banana" -> "banana";
            case "chicken_lover" -> "chickenlover";
            case "easy_peasy" -> "easypeasy";
            case "danger_zone" -> "dangerzone";
            case "the_boss" -> "boss";
            case "rage" -> "fury";
            default -> patch;
        };
        String itemId = "cs2lootbox:patch_" + patch;
        var item = new ModelItemDefinitionBuilder(itemId)
                .model("cs2lootbox:geo/patch_inspect.geo.json")
                .texture("cs2lootbox:textures/patches/patch_" + textureName + ".png")
                .name("item.cs2lootbox.patch." + patch)
                .rarityGrade(tier);
        // The imported mesh is a 0.1016-unit plane in Y/Z; face it toward the
        // viewer and center its lower-edge origin in every display context.
        item.gui(0, -6.4F, 0, 0, 90, 0, 8)
            .fixed(0, -6.4F, 0, 0, 90, 0, 8)
            .ground(0, 0, 0, 0, 90, 0, 4)
            .firstPersonRight(0, -3.2F, 0, 0, 90, 0, 4)
            .firstPersonLeft(0, -3.2F, 0, 0, 90, 0, 4)
            .thirdPersonRight(0, 0, 0, 0, 90, 0, 4)
            .thirdPersonLeft(0, 0, 0, 0, 90, 0, 4);
        LootboxRegistrationService.registerItem(item);
        double weight = LootRarityWeights.perEntry(LootRarityWeights.Profile.PATCH_PACK, tier);
        builder.loot(itemId, weight, loot -> loot.count(1).position(0, 8).scale(2.0F)
                .name("item.cs2lootbox.patch." + patch)
                .rarityGrade(tier)
                .rarity(tier.patchTranslationKey()));
    }
}
