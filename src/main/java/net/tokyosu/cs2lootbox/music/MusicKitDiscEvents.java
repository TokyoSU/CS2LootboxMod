package net.tokyosu.cs2lootbox.music;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.item.MusicKitItem;

/** Covers both vanilla record ejection and jukebox destruction before the item is sent to clients. */
@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID)
public final class MusicKitDiscEvents {
    
    private MusicKitDiscEvents() { }
    
    @SuppressWarnings("resource")
    @SubscribeEvent public static void onItemSpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity entity && entity.getItem().getItem() instanceof MusicKitItem item && !item.isBase()) {
            var kit = MusicKitSelection.restoreKit(entity.getItem());
            if (!kit.isEmpty()) entity.setItem(kit);
        }
    }
}
