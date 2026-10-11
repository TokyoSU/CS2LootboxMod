package net.tokyosu.cs2lootbox.client.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.item.MusicKitItem;
import net.tokyosu.cs2lootbox.mixin.client.CreativeModeTabsAccessor;
import net.tokyosu.cs2lootbox.music.MusicKitResources;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import java.util.Set;

@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MusicKitResourceEvents {
    private MusicKitResourceEvents() { }

    @SubscribeEvent public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Set<ResourceLocation>>() {
            @Override protected Set<ResourceLocation> prepare(ResourceManager resources, ProfilerFiller profiler) {
                return MusicKitResources.findAvailable(resources, LootboxRegistrationService.musicKits());
            }
            @Override protected void apply(Set<ResourceLocation> available, ResourceManager resources, ProfilerFiller profiler) {
                ClientMusicKitAvailability.update(available);
                CreativeModeTabsAccessor.cs2lootbox$invalidateContents(null);
                var minecraft = Minecraft.getInstance();
                if (minecraft.level != null && minecraft.player != null) {
                    CreativeModeTabs.tryRebuildTabContents(minecraft.level.enabledFeatures(),
                            minecraft.player.canUseGameMasterBlocks(), minecraft.level.registryAccess());
                }
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        var iterator = event.getEntries().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().getItem() instanceof MusicKitItem item
                    && (!item.isBase() || !ClientMusicKitAvailability.isAvailable(item.definition()))) iterator.remove();
        }
    }

    @SubscribeEvent public static void fallbackModels(ModelEvent.ModifyBakingResult event) {
        var available = MusicKitResources.findAvailable(Minecraft.getInstance().getResourceManager(),
                LootboxRegistrationService.musicKits());
        var fallback = event.getModels().get(new ModelResourceLocation(ResourceLocation.parse("minecraft:music_disc_13"), "inventory"));
        if (fallback == null) return;
        for (var kit : LootboxRegistrationService.musicKits()) {
            if (!available.contains(kit.id())) {
                event.getModels().put(new ModelResourceLocation(kit.id(), "inventory"), fallback);
            }
        }
    }
}
