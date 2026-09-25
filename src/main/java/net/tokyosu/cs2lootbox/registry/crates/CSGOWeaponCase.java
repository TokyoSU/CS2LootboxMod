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

        // Item display is renderer-owned so left/right hands are truly
        // independent. These values are the former lootbox_patch.json
        // display settings, now expressed directly through the builder.
        builder.thirdPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .thirdPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .ground(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.5F)
                .gui(0.0F, -3.0F, 0.0F, 30.0F, 135.0F, 0.0F, 1.5F)
                .fixed(0.0F, -1.5F, 0.0F, 0.0F, 135.0F, 0.0F, 1.5F);

        LootboxRegistrationService.register(builder);
    }
}
