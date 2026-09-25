package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.client.model.LootboxCaseItemModel;
import net.tokyosu.cs2lootbox.client.renderer.skinning.GeckoLibWeightedSkinRenderer;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelData;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
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
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);

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
                applyItemTransform(
                        poseStack,
                        selectItemTransform(item.getDefinition().itemTransforms(), transformType),
                        isLeftHandContext(transformType)
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
            SkinnedGeoModelData skinning = SkinnedGeoModelLoader.get(modelResource);
            GeckoLibWeightedSkinRenderer.render(
                    poseStack, model, skinning, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static @NotNull LootboxDefinition.ItemTransform selectItemTransform(
            @NotNull LootboxDefinition.ItemDisplayTransforms transforms,
            @NotNull ItemDisplayContext context) {
        return switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> transforms.firstPersonRight();
            case FIRST_PERSON_LEFT_HAND -> transforms.firstPersonLeft();
            case THIRD_PERSON_RIGHT_HAND -> transforms.thirdPersonRight();
            case THIRD_PERSON_LEFT_HAND -> transforms.thirdPersonLeft();
            case GROUND -> transforms.ground();
            case GUI -> transforms.gui();
            case FIXED -> transforms.fixed();
            default -> LootboxDefinition.ItemTransform.IDENTITY;
        };
    }

    private static boolean isLeftHandContext(@NotNull ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    /**
     * Applies the same conventions as vanilla ItemTransform.apply so values can
     * be moved 1:1 from a model JSON into KubeJS:
     * - translation is authored in model pixels and divided by 16
     * - left-hand transforms mirror X translation and Y/Z rotation
     * - rotation order is X -> Y -> Z, followed by scale
     *
     * Left and right still have fully independent KubeJS definitions; the
     * mirroring here only reproduces Minecraft's normal semantics for whichever
     * left-hand definition was selected.
     */
    private static void applyItemTransform(
            @NotNull PoseStack poseStack,
            @NotNull LootboxDefinition.ItemTransform transform,
            boolean leftHand) {
        float handSign = leftHand ? -1.0F : 1.0F;

        poseStack.translate(
                handSign * transform.translationX() / 16.0F,
                transform.translationY() / 16.0F,
                transform.translationZ() / 16.0F
        );

        if (transform.rotationX() != 0.0F
                || transform.rotationY() != 0.0F
                || transform.rotationZ() != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    transform.rotationX() * DEG_TO_RAD,
                    handSign * transform.rotationY() * DEG_TO_RAD,
                    handSign * transform.rotationZ() * DEG_TO_RAD
            ));
        }

        poseStack.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
    }
}
