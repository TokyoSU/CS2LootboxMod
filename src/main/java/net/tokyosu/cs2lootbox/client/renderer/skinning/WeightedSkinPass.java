package net.tokyosu.cs2lootbox.client.renderer.skinning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/** Shared post-animation skinning pass for item and GUI renderers. */
public final class WeightedSkinPass {
    private WeightedSkinPass() {}

    public static void render(ResourceLocation resource, PoseStack pose, BakedGeoModel model,
                              VertexConsumer buffer, int light, int overlay,
                              float red, float green, float blue, float alpha) {
        GeckoLibWeightedSkinRenderer.render(pose, model, SkinnedGeoModelLoader.get(resource), buffer,
                light, overlay, red, green, blue, alpha);
    }
}
