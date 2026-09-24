package net.tokyosu.cs2lootbox;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;
import org.jetbrains.annotations.NotNull;

/** Client-only lifecycle hooks. */
@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CS2LootBoxClientEvents {
    private CS2LootBoxClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> SkinnedGeoModelLoader.clear());
    }
}
