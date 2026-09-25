package net.tokyosu.cs2lootbox.registry.crates;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.registry.DefaultLootBoxRegistries;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import net.tokyosu.cs2lootbox.registry.ModSounds;

import java.util.Objects;

public class AgentDossier01 {
    public static final ResourceLocation CASE_ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "agents_dossier_01");

    public static synchronized void register() {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(CASE_ID);

        DefaultLootBoxRegistries.registerNewCrate(builder,
                "patch_envelope",
                "patch_envelope",
                "patch_envelope.geo.json",
                "patch_envelope.png",
                "patch_envelope.animation.json",
                "patch_envelope", false, false);

        // CS2 dossier presentation: a patch/fabric fall when it appears, then
        // the pin sound repeats for as long as the OPEN animation is running.
        builder.dropSound(Objects.requireNonNull(ModSounds.CASE_PATCH_FALL.getId()).toString())
                .openLoopSound(Objects.requireNonNull(ModSounds.CASE_PINS_FALL.getId()).toString());

        builder.position(70.0F, 150.0F).rotation(-60.0F, 170.0F, 0.0F);

        // Item display is renderer-owned so left/right hands are truly
        // independent. These values are the former lootbox_patch.json
        // display settings, now expressed directly through the builder.
        builder.thirdPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .thirdPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonLeft(0.5F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .ground(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .gui(0.0F, -7.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.8F)
                .fixed(0.0F, -1.5F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F);

        LootboxRegistrationService.register(builder);
    }
}
