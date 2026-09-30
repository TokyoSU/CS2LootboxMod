package net.tokyosu.cs2lootbox.registry;

import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import org.jetbrains.annotations.NotNull;

public class DefaultLootBoxRegistries {
    public static synchronized void registerNewCrate(@NotNull LootboxDefinitionBuilder builder, @NotNull String caseRegName, @NotNull String caseKeyRegName, @NotNull String modelPath, @NotNull String texturePath, @NotNull String animationPath, @NotNull String keyTexture, boolean requiresKey, boolean hasOpenIdleAnim) {
        builder.caseItem(CS2LootBoxMod.MOD_ID + ":" + caseRegName)
                .keyItem(CS2LootBoxMod.MOD_ID + ":" + caseKeyRegName)
                .itemJson(CS2LootBoxMod.MOD_ID + ":item/lootbox_renderer")
                .model(CS2LootBoxMod.MOD_ID + ":geo/" + modelPath)
                .texture(CS2LootBoxMod.MOD_ID + ":textures/lootbox/" + texturePath)
                .animation(CS2LootBoxMod.MOD_ID + ":animations/" + animationPath)
                .keyTexture(CS2LootBoxMod.MOD_ID + ":item/" + keyTexture)
                .requiresKey(requiresKey)
                .itemIdleAnimation("idle")
                .position(70.0F, 150.0F).rotation(-3.0F, 160.0F, -3.0F).scale(300.0F)
                .noOpenLoopSound()
                .singleOpenText("cs2lootbox.case_screen.single_open")
                .thirdPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .thirdPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .ground(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.5F)
                .gui(0.0F, -3.0F, 0.0F, 30.0F, 135.0F, 0.0F, 1.5F)
                .fixed(0.0F, -1.5F, 0.0F, 0.0F, 135.0F, 0.0F, 1.5F);
        
        if (hasOpenIdleAnim)
            builder.animations("fall", "idle", "open", "open_idle");
        else
            builder.animations("fall", "idle", "open");

        if (requiresKey)
            builder.openSound(CS2LootBoxMod.MOD_ID + ":case_unlock", 0.2F, 1.0F);
        else
            builder.openSound(CS2LootBoxMod.MOD_ID + ":case_unlock_immediate", 0.2F, 1.0F);
    }
    
    public static synchronized void registerNewPatch(@NotNull LootboxDefinitionBuilder builder, @NotNull String caseRegName, @NotNull String modelPath, @NotNull String texturePath, @NotNull String animationPath) {
        builder.caseItem(CS2LootBoxMod.MOD_ID + ":" + caseRegName)
                .itemJson(CS2LootBoxMod.MOD_ID + ":item/lootbox_renderer")
                .model(CS2LootBoxMod.MOD_ID + ":geo/" + modelPath)
                .texture(CS2LootBoxMod.MOD_ID + ":textures/lootbox/" + texturePath)
                .animation(CS2LootBoxMod.MOD_ID + ":animations/" + animationPath)
                .requiresKey(false)
                .itemIdleAnimation("idle")
                .animations("fall", "idle", "open")
                .dropSound(CS2LootBoxMod.MOD_ID + ":case_patch_fall", 0.2F, 1.0F)
                .openSound(CS2LootBoxMod.MOD_ID + ":case_pins_fall", 0.2F, 1.0F)
                .noOpenLoopSound()
                .singleOpenText("cs2lootbox.patch_screen.single_open")
                .position(240.0F, 440.0F).rotation(-60.0F, 170.0F, 0.0F).scale(600.0F)
                .thirdPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .thirdPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .firstPersonLeft(0.5F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .ground(0.0F, 0.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F)
                .gui(0.0F, -7.0F, 0.0F, 0.0F, 135.0F, 0.0F, 1.8F)
                .fixed(0.0F, -1.5F, 0.0F, 0.0F, 135.0F, 0.0F, 1.3F);
    }
    
    /**
     * ESport case use image instead of a model.
     * @param builder
     * @param caseRegName
     * @param modelPath
     * @param texturePath
     * @param animationPath
     */
    public static synchronized void registerNewESportCase(@NotNull LootboxDefinitionBuilder builder, @NotNull String caseRegName, @NotNull String caseKeyRegName, @NotNull String imageName, @NotNull String keyTexture, int imageWidth, int imageHeight) {
        builder.caseItem(CS2LootBoxMod.MOD_ID + ":" + caseRegName)
                .keyItem(CS2LootBoxMod.MOD_ID + ":" + caseKeyRegName)
                .image(CS2LootBoxMod.MOD_ID + ":textures/gui/cases/" + imageName, imageWidth, imageHeight)
                .keyTexture(CS2LootBoxMod.MOD_ID + ":item/" + keyTexture)
                .itemIdleAnimation("idle")
                .noDropSound()
                .openSound(CS2LootBoxMod.MOD_ID + ":case_unlock_immediate", 0.2F, 1.0F)
                .noOpenLoopSound()
                .singleOpenText("cs2lootbox.case_screen.single_open")
                .position(0.0F, -50.0F).rotation(0.0F, 0.0F, 0.0F).scale(300.0F)
                .thirdPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F)
                .thirdPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F)
                .firstPersonRight(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F)
                .firstPersonLeft(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F)
                .ground(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F)
                .gui(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F)
                .fixed(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1F);

    }
}
