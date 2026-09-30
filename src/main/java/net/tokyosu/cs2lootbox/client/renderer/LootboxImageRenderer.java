package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;

/** A centered, unlit image plane shared by the GUI and case item renderer. */
public final class LootboxImageRenderer {
    private LootboxImageRenderer() {}

    public static void render(PoseStack pose, MultiBufferSource buffers,
                              LootboxDefinition.PreviewImage image) {
        VertexConsumer vertices = buffers.getBuffer(ForgeRenderTypes.getUnlitTranslucent(image.texture(), false));
        float halfHeight = 0.5F * image.height() / image.width();
        PoseStack.Pose matrix = pose.last();
        int light = LightTexture.FULL_BRIGHT;
        vertex(vertices, matrix, -0.5F, -halfHeight, 0, 1, light);
        vertex(vertices, matrix, 0.5F, -halfHeight, 1, 1, light);
        vertex(vertices, matrix, 0.5F, halfHeight, 1, 0, light);
        vertex(vertices, matrix, -0.5F, halfHeight, 0, 0, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose matrix,
                               float x, float y, float u, float v, int light) {
        vertices.vertex(matrix.pose(), x, y, 0).color(255, 255, 255, 255)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(matrix.normal(), 0, 0, 1).endVertex();
    }
}
