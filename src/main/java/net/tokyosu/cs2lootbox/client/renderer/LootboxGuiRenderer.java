package net.tokyosu.cs2lootbox.client.renderer;

import net.tokyosu.cs2lootbox.client.renderer.skinning.WeightedSkinPass;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.client.animation.LootboxAnimatable;
import net.tokyosu.cs2lootbox.client.model.LootboxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * GeckoLib renderer used by the LDLib lootbox preview.
 *
 * <p>GeckoLib remains the animation engine. Rigid geometry is rendered by the
 * normal GeckoLib/GeckoMesh path. If a .geo.json also contains the
 * {@code cs2_skinning} extension emitted by our Blockbench importer, the
 * original glTF vertex weights are blended from GeckoLib's evaluated bone pose
 * and submitted through the same render pass.</p>
 */
public final class LootboxGuiRenderer extends GeoObjectRenderer<LootboxAnimatable> {
    private boolean deferGeometry;
    public LootboxGuiRenderer() {
        super(new LootboxModel());
    }

    @Override
    public @NotNull RenderType getRenderType(
            @NotNull LootboxAnimatable animatable,
            @NotNull ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        // Keep the existing unlit UI presentation. Weighted vertices are sent
        // to this same VertexConsumer, so they use exactly the same texture,
        // blending, depth state and lightmap behaviour as the rigid model.
        return ForgeRenderTypes.getUnlitTranslucent(texture, false);
    }

    @Override
    public void actuallyRender(
            PoseStack poseStack,
            LootboxAnimatable animatable,
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
        String clip = switch (animatable.getAnimationState()) {
            case IDLE -> animatable.getDefinition().animations().idle();
            case OPEN_IDLE -> animatable.getDefinition().animations().hasOpenIdle()
                    ? animatable.getDefinition().animations().openIdle() : null;
            default -> null;
        };
        if (!isReRender && clip != null && getRenderLayers().isEmpty()
                && CaseGeometryCache.supports(poseStack) && CaseGeometryCache.supports(model.topLevelBones()) && SkinnedGeoModelLoader.get(resource).isEmpty()
                && StaticItemAnimation.isStatic(animatable.getDefinition().animation(), clip)) {
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
            if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
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
        // This evaluates GeckoLib's controllers/keyframes and renders ordinary
        // cubes/GeckoMesh geometry first. The GeoBone state is then the exact
        // pose we need for weighted skinning below.
        super.actuallyRender(
                poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        // GeckoLib re-render passes are normally render layers. Emitting the
        // base weighted mesh again there would duplicate it, so skin only on
        // the primary model pass.
        if (!isReRender) {
            ResourceLocation modelResource = getGeoModel().getModelResource(animatable, this);
            WeightedSkinPass.render(
                    modelResource, poseStack, model, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    @Override
    public void renderRecursively(PoseStack pose, LootboxAnimatable animatable, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer vertices, boolean reRender,
                                  float tick, int light, int overlay, float r, float g, float b, float a) {
        if (!deferGeometry) super.renderRecursively(pose, animatable, bone, type, buffers, vertices,
                reRender, tick, light, overlay, r, g, b, a);
    }

    /**
     * GeoObjectRenderer is intended for generic animatables and GeckoLib
     * recommends giving each rendered object a stable instance id.
     */
    @Override
    public long getInstanceId(@NotNull LootboxAnimatable animatable) {
        return System.identityHashCode(animatable);
    }
}
