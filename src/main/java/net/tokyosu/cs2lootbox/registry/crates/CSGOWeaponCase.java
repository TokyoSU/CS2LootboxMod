package net.tokyosu.cs2lootbox.registry.crates;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.registry.DefaultLootBoxRegistries;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;

@SuppressWarnings("SpellCheckingInspection")
public final class CSGOWeaponCase {
    public static final ResourceLocation CASE_ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "csgo_case_weapon");

    public static synchronized void register() {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID);

        DefaultLootBoxRegistries.registerNewCrate(builder,
                "csgo_case_weapon",
                "csgo_case_weapon_key",
                "csgo_drop_crate_armsdeal1.geo.json",
                "csgo_drop_crate_armsdeal1.png",
                "csgo_drop_crate_armsdeal1.animation.json",
                "weapon_case_key", true, false);

        LootboxRegistrationService.register(builder);
    }
}
