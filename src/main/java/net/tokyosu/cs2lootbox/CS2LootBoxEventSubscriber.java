package net.tokyosu.cs2lootbox;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.registry.LootboxRegistry;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CS2LootBoxEventSubscriber {
    private CS2LootBoxEventSubscriber() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabContents(@NotNull BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            return;
        }

        for (LootboxDefinition definition : LootboxRegistry.values()) {
            acceptIfPresent(event, definition.caseItemId());
            if (definition.requiresKey()) {
                acceptIfPresent(event, definition.keyItemId());
            }
        }
    }

    private static void acceptIfPresent(@NotNull BuildCreativeModeTabContentsEvent event, @NotNull net.minecraft.resources.ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item != null) {
            event.accept(item);
        }
    }
}
