package net.tokyosu.cs2lootbox.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Built-in CS2-style UI sounds shipped by the mod.
 *
 * Keeping the SoundEvents in Java means crate definitions can use them as
 * defaults and KubeJS packs do not have to register the same sounds again.
 */
public final class ModSounds {
    public static final @NotNull DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CS2LootBoxMod.MOD_ID);

    public static final RegistryObject<SoundEvent> BUTTON_CLICK = register("buttonclick");

    public static final RegistryObject<SoundEvent> CASE_AWARDED_COMMON = register("case_awarded_0_common");
    public static final RegistryObject<SoundEvent> CASE_AWARDED_UNCOMMON = register("case_awarded_1_uncommon");
    public static final RegistryObject<SoundEvent> CASE_AWARDED_RARE = register("case_awarded_2_rare");
    public static final RegistryObject<SoundEvent> CASE_AWARDED_MYTHICAL = register("case_awarded_3_mythical");
    public static final RegistryObject<SoundEvent> CASE_AWARDED_LEGENDARY = register("case_awarded_4_legendary");
    public static final RegistryObject<SoundEvent> CASE_AWARDED_ANCIENT = register("case_awarded_5_ancient");

    public static final RegistryObject<SoundEvent> CASE_DROP = register("case_drop");
    public static final RegistryObject<SoundEvent> CASE_PATCH_FALL = register("case_patch_fall");
    public static final RegistryObject<SoundEvent> CASE_PINS_FALL = register("case_pins_fall");
    public static final RegistryObject<SoundEvent> CASE_PURCHASE_KEY = register("case_purchase_key");

    public static final RegistryObject<SoundEvent> CASE_REVEAL_RARE = register("case_reveal_rare");
    public static final RegistryObject<SoundEvent> CASE_REVEAL_MYTHICAL = register("case_reveal_mythical");
    public static final RegistryObject<SoundEvent> CASE_REVEAL_LEGENDARY = register("case_reveal_legendary");
    public static final RegistryObject<SoundEvent> CASE_REVEAL_ANCIENT = register("case_reveal_ancient");

    public static final RegistryObject<SoundEvent> CASE_UNLOCK = register("case_unlock");
    public static final RegistryObject<SoundEvent> CASE_UNLOCK_IMMEDIATE = register("case_unlock_immediate");

    public static final RegistryObject<SoundEvent> CRATE_ITEM_SCROLL = register("csgo_ui_crate_item_scroll");
    public static final RegistryObject<SoundEvent> CRATE_RESULT = register("csgo_ui_crate_result");
    public static final RegistryObject<SoundEvent> MENU_ACCEPT = register("menu_accept");
    public static final RegistryObject<SoundEvent> MENU_INVALID = register("menu_invalid");

    private ModSounds() {
    }

    public static void register(@NotNull IEventBus modEventBus) {
        SOUND_EVENTS.register(Objects.requireNonNull(modEventBus, "modEventBus"));
    }

    private static @NotNull RegistryObject<SoundEvent> register(@NotNull String name) {
        return SOUND_EVENTS.register(name, () -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, name);
            return SoundEvent.createVariableRangeEvent(id);
        });
    }
}
