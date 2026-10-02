package net.tokyosu.cs2lootbox.client.renderer;

import net.tokyosu.cs2lootbox.client.renderer.skinning.WeightedSkinPass;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.client.model.LootboxCaseItemModel;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib ItemStack renderer for lootbox cases, including the optional exact
 * glTF weighted-skin extension used by CS2/Source 2 assets.
 *
 * Item display transforms are applied here instead of relying on vanilla model
 * JSON display transforms. This makes FIRST_PERSON_LEFT_HAND and
 * FIRST_PERSON_RIGHT_HAND fully independent for GeckoLib custom-rendered items
 * and lets KubeJS own all seven useful item display contexts.
 */
public final class LootboxCaseItemRenderer extends GeoItemRenderer<LootboxCaseItem> {
    public LootboxCaseItemRenderer() {
        super(new LootboxCaseItemModel());
    }

    @Override
    public void renderByItem(
            @NotNull ItemStack stack,
            @NotNull ItemDisplayContext transformType,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();
        try {
            if (stack.getItem() instanceof LootboxCaseItem item) {
                // ItemRenderer has already applied its custom-renderer centering
                // translation (-0.5, -0.5, -0.5) before renderByItem is called.
                // Vanilla JSON display transforms are applied *before* that step.
                // Undo the centering, apply our KubeJS transform at the same point
                // vanilla would, then restore the centering before GeckoLib renders.
                poseStack.translate(0.5F, 0.5F, 0.5F);
                ModelItemTransforms.applyItemTransform(
                        poseStack,
                        ModelItemTransforms.selectItemTransform(item.getDefinition().itemTransforms(), transformType),
                        ModelItemTransforms.isLeftHandContext(transformType)
                );
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                if (item.getDefinition().image() != null) {
                    poseStack.translate(0.5F, 0.5F, 0.5F);
                    LootboxImageRenderer.render(poseStack, bufferSource, item.getDefinition().image());
                    return;
                }
            }

            super.renderByItem(
                    stack,
                    transformType,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public @NotNull RenderType getRenderType(
            @NotNull LootboxCaseItem animatable,
            @NotNull ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        // FULL_BRIGHT only maxes the lightmap; it does not disable Minecraft's
        // directional entity shading. The lootbox UI already uses Forge's truly
        // unlit RenderType, so use the exact same path for first-person hands.
        if (renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || renderPerspective == ItemDisplayContext.GUI) {
            return ForgeRenderTypes.getUnlitTranslucent(texture, false);
        }
        return getGeoModel().getRenderType(animatable, texture);
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
            WeightedSkinPass.render(
                    modelResource, poseStack, model, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

}
