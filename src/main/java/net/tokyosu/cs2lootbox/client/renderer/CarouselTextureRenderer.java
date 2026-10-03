package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** The existing nine-tap texture smear in one ordered draw instead of nine flushes. */
public final class CarouselTextureRenderer {
    private CarouselTextureRenderer() {}

    public static void blurred(GuiGraphics graphics, ResourceLocation texture, int x, int y,
            int width, int height, float alpha, float amount, int radius) {
        if (texture == null || width <= 0 || height <= 0 || alpha <= 0) return;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GuiArtworkShader.get() != null ? GuiArtworkShader::get : GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float tapAlpha = alpha * (0.10F * amount);
        quad(builder, matrix, x - radius, y, width, height, tapAlpha);
        quad(builder, matrix, x + radius, y, width, height, tapAlpha);
        quad(builder, matrix, x, y - radius, width, height, tapAlpha);
        quad(builder, matrix, x, y + radius, width, height, tapAlpha);
        quad(builder, matrix, x - radius, y - radius, width, height, tapAlpha);
        quad(builder, matrix, x + radius, y - radius, width, height, tapAlpha);
        quad(builder, matrix, x - radius, y + radius, width, height, tapAlpha);
        quad(builder, matrix, x + radius, y + radius, width, height, tapAlpha);
        quad(builder, matrix, x, y, width, height, alpha * (1 - 0.78F * amount));
        try { BufferUploader.drawWithShader(builder.end()); }
        finally {
            // GuiGraphics.flush restores depth testing after its ordinary blits as well.
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, int x, int y, int w, int h, float alpha) {
        builder.vertex(matrix, x, y + h, 0).uv(0, 1).color(1F, 1F, 1F, alpha).endVertex();
        builder.vertex(matrix, x + w, y + h, 0).uv(1, 1).color(1F, 1F, 1F, alpha).endVertex();
        builder.vertex(matrix, x + w, y, 0).uv(1, 0).color(1F, 1F, 1F, alpha).endVertex();
        builder.vertex(matrix, x, y, 0).uv(0, 0).color(1F, 1F, 1F, alpha).endVertex();
    }
}
