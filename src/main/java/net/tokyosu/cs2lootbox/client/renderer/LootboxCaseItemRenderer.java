package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.client.model.LootboxCaseItemModel;
import net.tokyosu.cs2lootbox.client.renderer.skinning.GeckoLibWeightedSkinRenderer;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelData;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib ItemStack renderer for lootbox cases, including the optional exact
 * glTF weighted-skin extension used by CS2/Source 2 assets.
 */
public final class LootboxCaseItemRenderer extends GeoItemRenderer<LootboxCaseItem> {
    public LootboxCaseItemRenderer() {
        super(new LootboxCaseItemModel());
    }

    @Override
    public void actuallyRender(
            PoseStack poseStack,
            LootboxCaseItem animatable,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        super.actuallyRender(
                poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        if (!isReRender) {
            ResourceLocation modelResource = getGeoModel().getModelResource(animatable, this);
            SkinnedGeoModelData skinning = SkinnedGeoModelLoader.get(modelResource);
            GeckoLibWeightedSkinRenderer.render(
                    poseStack, model, skinning, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
