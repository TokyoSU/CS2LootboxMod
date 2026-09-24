package net.tokyosu.cs2lootbox.registry.crates;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.registry.DefaultLootBoxRegistries;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;

public class AgentDossier01 {
    public static final ResourceLocation CASE_ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "agents_dossier_01");

    public static synchronized void register() {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID);

        DefaultLootBoxRegistries.registerNewCrate(builder,
                "agents_dossier_01",
                "agents_dossier_01_key",
                "agents_dossier_01.geo.json",
                "agents_dossier_01.png",
                "agents_dossier_01.animation.json",
                "agents_dossier_01_key", false, false);

        LootboxRegistrationService.register(builder);
    }
}
