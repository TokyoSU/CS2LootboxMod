package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

/** Exercise the real GPU cache while alternating hand, hotbar and faded preview draws. */
public final class HeldCaseCacheCheck {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        // Initialize vanilla registries without Forge's network bootstrap, which
        // requires event-class transformers unavailable in a standalone JavaExec.
        var flag = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
        flag.setAccessible(true); flag.setBoolean(null, true);
        net.minecraft.core.registries.BuiltInRegistries.bootStrap();
        require(GLFW.glfwInit(), "GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(32, 32, "Cache regression check", 0, 0);
        require(window != 0, "Hidden OpenGL context unavailable");
        try (ZipFile vanilla = new ZipFile(args[1])) {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            RenderSystem.initRenderThread(); RenderSystem.initGameThread(false);
            var root = Path.of(args[0], "src/main/resources");
            var pack = new PathPackResources("cache-check", root, false);
            ResourceProvider provider = location -> {
                String path = "assets/" + location.getNamespace() + "/" + location.getPath();
                Path local = root.resolve(path);
                if (Files.exists(local)) return Optional.of(new Resource(pack, () -> Files.newInputStream(local)));
                var entry = vanilla.getEntry(path);
                return entry == null ? Optional.empty() : Optional.of(new Resource(pack, () -> vanilla.getInputStream(entry)));
            };
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableCull(); RenderSystem.disableDepthTest();
            try (var shader = new ShaderInstance(provider,
                    ResourceLocation.fromNamespaceAndPath("cs2lootbox", "gui_artwork"), DefaultVertexFormat.POSITION_TEX_COLOR)) {
                int texture = GL11.glGenTextures();
                var white = MemoryUtil.memAlloc(4);
                white.put(new byte[]{-1, -1, -1, -1}).flip();
                RenderSystem.bindTexture(texture);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, white);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                MemoryUtil.memFree(white);
                RenderType type = new RenderType("cache-check", DefaultVertexFormat.POSITION_TEX_COLOR,
                        VertexFormat.Mode.QUADS, 256, false, false, () -> {
                            RenderSystem.setShader(() -> shader); RenderSystem.setShaderTexture(0, texture);
                            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
                        }, () -> {}) {};
                GeoBone bone = new GeoBone(null, "case", false, 0D, false, false);
                BakedGeoModel model = new BakedGeoModel(List.of(bone), null);
                int[] bakes = {0};
                PoseStack pose = new PoseStack();
                for (int frame = 0; frame < 200; frame++) {
                    draw(pose, model, type, 12345, 1, bakes); // Hand: world light.
                    draw(pose, model, type, 15728880, 1, bakes); // Hotbar: full brightness.
                    draw(pose, model, type, 15728880, 0.5F, bakes); // UI: different opacity.
                }
                require(bakes[0] == 3, "600 alternating draws uploaded " + bakes[0] + " meshes; expected 3");
                bone.setRotX(0.2F);
                draw(pose, model, type, 12345, 1, bakes);
                require(bakes[0] == 4, "A changed bone pose must rebake");
                for (int i = 0; i < 32; i++) draw(pose, model, type, i, 1, bakes);
                draw(pose, model, type, 12345, 1, bakes);
                require(bakes[0] == 37, "Evicted mesh must rebake");
                HeldCaseMeshCache.clear();
                draw(pose, model, type, 12345, 1, bakes);
                require(bakes[0] == 38, "Reload must rebake");
                HeldCaseMeshCache.clear(); GL11.glDeleteTextures(texture);
                require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error");
                System.out.println("PASS: 600 alternating hand/hotbar/faded draws require 3 uploads; pose changes, eviction and reload rebake correctly");
            }
        } finally { GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }

    private static void draw(PoseStack pose, BakedGeoModel model, RenderType type, int light, float alpha, int[] bakes) {
        HeldCaseMeshCache.render(pose, model, type, light, 0, 1, 1, 1, alpha, (local, out) -> {
            bakes[0]++;
            out.vertex(-0.5, -0.5, 0).uv(0, 0).color(1F, 1F, 1F, alpha).endVertex();
            out.vertex(0.5, -0.5, 0).uv(1, 0).color(1F, 1F, 1F, alpha).endVertex();
            out.vertex(0.5, 0.5, 0).uv(1, 1).color(1F, 1F, 1F, alpha).endVertex();
            out.vertex(-0.5, 0.5, 0).uv(0, 1).color(1F, 1F, 1F, alpha).endVertex();
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
