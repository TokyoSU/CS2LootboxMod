package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.item.ModelItem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashSet;

/** Full-detail artwork at twice the physical display resolution; nine blur taps in one draw.
 * Animated textures, foil, custom passes and translucent 3D meshes retain their live renderer.
 * Snapshot alpha is premultiplied: multiply RGB and alpha together, then blend with ONE.
 */
public final class CarouselItemArtwork {
    private static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final LinkedHashSet<Image> IMAGES = new LinkedHashSet<>();
    private static long bytes;
    private CarouselItemArtwork() {}

    public static boolean render(GuiGraphics graphics, ItemStack stack, int cx, int cy,
                                 float scale, float alpha, float blur, int radius) {
        if (GuiArtworkShader.getPremultiplied() == null || RenderSystem.getShaderFogEnd() < 10000) return false;
        Matrix4f pose = graphics.pose().last().pose();
        // Runtime canvas transforms are axis-aligned. Other projections retain the live path.
        if (pose.m01() != 0 || pose.m10() != 0 || pose.m20() != 0 || pose.m21() != 0
                || pose.m00() <= 0 || pose.m11() <= 0) return false;
        final boolean[] drawn = {false};
        ItemGuiRenderContext.render(ItemGuiRenderContext.Purpose.CAROUSEL, () -> {
            StaticGuiItemCache.Mesh mesh = StaticGuiItemCache.mesh(stack);
            if (mesh == null || !supports(mesh, (ModelItem) stack.getItem())) return;
            var bounds = mesh.bounds;
            float densityX = pose.m00() * scale * (float)Minecraft.getInstance().getWindow().getGuiScale() * 2F;
            float densityY = pose.m11() * scale * (float)Minecraft.getInstance().getWindow().getGuiScale() * 2F;
            int width = (int)Math.ceil((bounds.maxX - bounds.minX) * densityX) + 4;
            int height = (int)Math.ceil((bounds.maxY - bounds.minY) * densityY) + 4;
            // Never downscale an oversized preview. Use the original rendering path instead.
            int maximum = Math.min(2048, RenderSystem.maxSupportedTextureSize());
            if (width <= 4 || height <= 4 || width > maximum || height > maximum) return;
            graphics.flush();
            Image image = mesh.artwork;
            if (image == null || image.closed || image.target.width != width || image.target.height != height) {
                if (image != null) image.close();
                while (bytes + width * (long)height * 8 > MAX_BYTES && !IMAGES.isEmpty()) IMAGES.iterator().next().close();
                mesh.artwork = image = bake(mesh, width, height, densityX, densityY);
            }
            IMAGES.remove(image); IMAGES.add(image);
            draw(graphics, image, cx - 8 * scale, cy - 8 * scale, scale, alpha, blur, radius);
            drawn[0] = true;
        });
        return drawn[0];
    }

    private static boolean supports(StaticGuiItemCache.Mesh mesh, ModelItem item) {
        if (mesh.artworkSupported != null) return mesh.artworkSupported;
        var b = mesh.bounds;
        boolean supported = b.supported && b.vertices > 0 && Float.isFinite(b.depth)
                && mesh.parts.size() == 1 && mesh.parts.get(0).type()
                == ForgeRenderTypes.getUnlitTranslucent(item.getDefinition().texture(), false);
        var manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation texture = item.getDefinition().texture();
        if (manager.getResource(ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + ".mcmeta")).isPresent()) supported = false;
        // Flattening overlapping translucent surfaces changes their compositing.
        // Flat patch artwork (two triangles) is safe; translucent 3D models are not.
        if (supported && b.vertices > 8) {
            try (var stream = manager.getResourceOrThrow(texture).open(); NativeImage image = NativeImage.read(stream)) {
                outer: for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                    int a = image.getPixelRGBA(x, y) >>> 24;
                    if (a >= 26 && a < 255) { supported = false; break outer; }
                }
            } catch (Exception ignored) { supported = false; }
        }
        return mesh.artworkSupported = supported;
    }

    private static Image bake(StaticGuiItemCache.Mesh mesh, int width, int height, float dx, float dy) {
        int drawTarget = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readTarget = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] scissorBox = new int[4]; GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        float[] color = RenderSystem.getShaderColor().clone();
        TextureTarget target = null;
        try {
            RenderSystem.disableScissor();
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            target.setFilterMode(GL11.GL_LINEAR);
            float left = mesh.bounds.minX - 2 / dx, top = mesh.bounds.minY - 2 / dy;
            float right = mesh.bounds.maxX + 2 / dx, bottom = mesh.bounds.maxY + 2 / dy;
            float depth = mesh.bounds.depth + 1000;
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(left, right, bottom, top, -depth, depth), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            StaticGuiItemCache.draw(mesh, new Matrix4f());
            Image image = new Image(target, left, top, right, bottom);
            IMAGES.add(image); bytes += image.bytes;
            return image;
        } catch (RuntimeException | Error failure) {
            if (target != null) target.destroyBuffers();
            throw failure;
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            RenderSystem.depthMask(false); RenderSystem.disableDepthTest();
        }
    }

    private static void draw(GuiGraphics graphics, Image image, float x, float y, float scale,
                             float alpha, float blur, int radius) {
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GuiArtworkShader::getPremultiplied); RenderSystem.setShaderTexture(0, image.target.getColorTextureId());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();
        float tap = alpha * 0.10F * blur;
        quad(builder, matrix, image, x - radius, y, scale, tap);
        quad(builder, matrix, image, x + radius, y, scale, tap);
        quad(builder, matrix, image, x, y - radius, scale, tap);
        quad(builder, matrix, image, x, y + radius, scale, tap);
        quad(builder, matrix, image, x - radius, y - radius, scale, tap);
        quad(builder, matrix, image, x + radius, y - radius, scale, tap);
        quad(builder, matrix, image, x - radius, y + radius, scale, tap);
        quad(builder, matrix, image, x + radius, y + radius, scale, tap);
        quad(builder, matrix, image, x, y, scale, alpha * (1 - 0.78F * blur));
        try { BufferUploader.drawWithShader(builder.end()); }
        finally { RenderSystem.defaultBlendFunc(); }
    }

    private static void quad(BufferBuilder b, Matrix4f m, Image image, float x, float y, float scale, float a) {
        float left = x + image.left * scale, right = x + image.right * scale;
        float top = y + image.top * scale, bottom = y + image.bottom * scale;
        b.vertex(m, left, bottom, 0).uv(0, 0).color(a, a, a, a).endVertex();
        b.vertex(m, right, bottom, 0).uv(1, 0).color(a, a, a, a).endVertex();
        b.vertex(m, right, top, 0).uv(1, 1).color(a, a, a, a).endVertex();
        b.vertex(m, left, top, 0).uv(0, 1).color(a, a, a, a).endVertex();
    }

    static final class Image {
        final TextureTarget target;
        final float left, top, right, bottom;
        final long bytes;
        boolean closed;
        Image(TextureTarget target, float left, float top, float right, float bottom) {
            this.target = target; this.left = left; this.top = top; this.right = right; this.bottom = bottom;
            this.bytes = target.width * (long)target.height * 8;
        }
        void close() {
            if (closed) return;
            int drawTarget = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int readTarget = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int id = target.frameBufferId;
            target.destroyBuffers(); closed = true;
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget == id ? 0 : drawTarget);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget == id ? 0 : readTarget);
            IMAGES.remove(this); CarouselItemArtwork.bytes -= bytes;
        }
    }
}
