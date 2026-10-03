package net.tokyosu.cs2lootbox;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.client.renderer.GuiArtworkShader;
import java.io.IOException;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;
import net.tokyosu.cs2lootbox.client.renderer.skinning.GeckoLibWeightedSkinRenderer;
import net.tokyosu.cs2lootbox.client.renderer.StaticGuiItemCache;
import net.tokyosu.cs2lootbox.client.renderer.HeldCaseMeshCache;
import net.tokyosu.cs2lootbox.client.renderer.CaseGeometryCache;
import net.tokyosu.cs2lootbox.client.renderer.ClientRenderRevision;
import org.jetbrains.annotations.NotNull;

/** Client-only lifecycle hooks. */
@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CS2LootBoxClientEvents {
    private CS2LootBoxClientEvents() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "gui_artwork"),
                DefaultVertexFormat.POSITION_TEX_COLOR), GuiArtworkShader::set);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(CS2LootBoxMod.MOD_ID, "gui_artwork_premultiplied"),
                DefaultVertexFormat.POSITION_TEX_COLOR), GuiArtworkShader::setPremultiplied);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            SkinnedGeoModelLoader.clear();
            GeckoLibWeightedSkinRenderer.clear();
            StaticGuiItemCache.clear();
            HeldCaseMeshCache.clear();
            CaseGeometryCache.clear();
            ClientRenderRevision.invalidate();
        });
    }
}
