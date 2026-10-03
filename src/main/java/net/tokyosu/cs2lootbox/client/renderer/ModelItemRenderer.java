package net.tokyosu.cs2lootbox.client.renderer;

import net.tokyosu.cs2lootbox.client.renderer.skinning.WeightedSkinPass;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.client.model.StaticItemModel;
import net.tokyosu.cs2lootbox.item.ModelItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib ItemStack renderer for static model items, including the optional exact
 * glTF weighted-skin extension used by CS2/Source 2 assets.
 *
 * Item display transforms are applied here instead of relying on vanilla model
 * JSON display transforms. This makes FIRST_PERSON_LEFT_HAND and
 * FIRST_PERSON_RIGHT_HAND fully independent for GeckoLib custom-rendered items
 * and lets KubeJS own all seven useful item display contexts.
 */
public final class ModelItemRenderer extends GeoItemRenderer<ModelItem> {
    private boolean deferGeometry;
    public ModelItemRenderer() {
        super(new StaticItemModel());
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
            if (stack.getItem() instanceof ModelItem item) {
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
            @NotNull ModelItem animatable,
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
            ModelItem animatable,
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
        ResourceLocation resource = getGeoModel().getModelResource(animatable, this);
        if (!isReRender && getRenderLayers().isEmpty() && CaseGeometryCache.supports(poseStack) && CaseGeometryCache.supports(model.topLevelBones())
                && SkinnedGeoModelLoader.get(resource).isEmpty()) {
            deferGeometry = true;
            try {
                super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                        false, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            } finally { deferGeometry = false; }
            LocalGeometry geometry = CaseGeometryCache.get(model, (localPose, vertices) -> {
                for (GeoBone root : model.topLevelBones()) {
                    super.renderRecursively(localPose, animatable, root, renderType, bufferSource, vertices,
                            true, partialTick, packedLight, packedOverlay, 1, 1, 1, 1);
                }
            });
            if (currentItemStack != null && !currentItemStack.hasFoil()
                    && bufferSource instanceof MultiBufferSource.BufferSource immediate
                    && renderType == ForgeRenderTypes.getUnlitTranslucent(getTextureLocation(animatable), false)) {
                immediate.endBatch();
                try {
                    HeldCaseMeshCache.render(poseStack, model, renderType, packedLight, packedOverlay,
                            red, green, blue, alpha, (localPose, vertices) -> geometry.emit(localPose, vertices,
                                    packedLight, packedOverlay, red, green, blue, alpha));
                } finally { immediate.getBuffer(renderType); }
            } else {
                geometry.emit(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
            return;
        }
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

    @Override
    public void renderRecursively(PoseStack pose, ModelItem animatable, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer vertices, boolean reRender,
                                  float tick, int light, int overlay, float r, float g, float b, float a) {
        if (!deferGeometry) super.renderRecursively(pose, animatable, bone, type, buffers, vertices,
                reRender, tick, light, overlay, r, g, b, a);
    }

}

