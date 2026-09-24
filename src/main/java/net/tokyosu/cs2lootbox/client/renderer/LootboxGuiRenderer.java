package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.client.animation.LootboxAnimatable;
import net.tokyosu.cs2lootbox.client.model.LootboxModel;
import net.tokyosu.cs2lootbox.client.renderer.skinning.GeckoLibWeightedSkinRenderer;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelData;
import net.tokyosu.cs2lootbox.client.renderer.skinning.SkinnedGeoModelLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
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
            SkinnedGeoModelData skinning = SkinnedGeoModelLoader.get(modelResource);
            GeckoLibWeightedSkinRenderer.render(
                    poseStack, model, skinning, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
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
