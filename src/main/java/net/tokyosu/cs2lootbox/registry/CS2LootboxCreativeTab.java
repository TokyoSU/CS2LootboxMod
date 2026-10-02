package net.tokyosu.cs2lootbox.registry;

import dev.latvian.mods.kubejs.item.creativetab.CreativeTabBuilder;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;

/** KubeJS-backed tab populated after item registries are ready. */
public final class CS2LootboxCreativeTab {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "main");
    private CS2LootboxCreativeTab() {}

    public static void register() {
        CreativeTabBuilder tab = new CreativeTabBuilder(ID);
        tab.displayName(Component.translatable("itemGroup.cs2lootbox"));
        tab.icon(() -> {
            var id = ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "csgo_case_weapon");
            var item = ForgeRegistries.ITEMS.getValue(id);
            return new ItemStack(item == null || item == Items.AIR ? Items.CHEST : item);
        });
        tab.content(showRestricted -> LootboxRegistrationService.creativeItemIds().stream()
                .filter(ForgeRegistries.ITEMS::containsKey)
                .map(ForgeRegistries.ITEMS::getValue)
                .filter(item -> item != null && item != Items.AIR)
                .map(ItemStack::new)
                .toArray(ItemStack[]::new));
        RegistryInfo.CREATIVE_MODE_TAB.addBuilder(tab);
    }
}
