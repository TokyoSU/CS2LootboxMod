package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.tokyosu.cs2lootbox.item.ModelItem;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.texture.AnimatableTexture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded GPU geometry cache for our controller-free ModelItems only.
 * Textures, including animated resource-pack textures, remain live and full resolution.
 * Third-party renderers, foil and animated cases always use the vanilla path.
 */
public final class StaticGuiItemCache {
    private static final int MAX_ENTRIES = 64;
    private static final Map<Key, Mesh> CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private StaticGuiItemCache() {}

    public static boolean render(GuiGraphics graphics, ItemStack stack) {
        if (!(stack.getItem() instanceof ModelItem item) || stack.hasFoil()) return false;
        if (!(IClientItemExtensions.of(stack).getCustomRenderer() instanceof ModelItemRenderer renderer)
                || !renderer.getRenderLayers().isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        BakedModel baked = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        if (!baked.isCustomRenderer()) return false;
        Key key = new Key(item, ItemGuiRenderContext.currentPurpose(), baked);
        Mesh mesh = CACHE.get(key);
        if (mesh == null) {
            float[] color = RenderSystem.getShaderColor().clone();
            try { mesh = bake(stack, baked); }
            finally { RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]); }
            CACHE.put(key, mesh);
            if (CACHE.size() > MAX_ENTRIES) {
                var oldest = CACHE.entrySet().iterator();
                oldest.next().getValue().close();
                oldest.remove();
            }
        }
        // GeckoLib textures can advance independently from this item's static geometry.
        AnimatableTexture.setAndUpdate(item.getDefinition().texture());
        // Match GuiGraphics.renderItem's lighting and local transforms exactly.
        boolean flat = !baked.usesBlockLight();
        if (flat) Lighting.setupForFlatItems();
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(graphics.pose().last().pose());
        try {
            for (Part part : mesh.parts) {
                part.type.setupRenderState();
                try {
                    part.vertices.bind();
                    part.vertices.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                } finally {
                    VertexBuffer.unbind();
                    part.type.clearRenderState();
                }
            }
        } finally {
            if (flat) Lighting.setupFor3DItems();
        }
        return true;
    }

    private static Mesh bake(ItemStack stack, BakedModel baked) {
        PoseStack pose = new PoseStack();
        pose.translate(8, 8, 150);
        pose.mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));
        pose.scale(16, 16, 16);
        Capture capture = new Capture();
        List<Part> parts = new ArrayList<>();
        try {
            Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.GUI, false,
                    pose, capture, 15728880, OverlayTexture.NO_OVERLAY, baked);
            for (var entry : capture.builders.entrySet()) {
                if (entry.getKey().mode() == VertexFormat.Mode.QUADS)
                    entry.getValue().setQuadSorting(RenderSystem.getVertexSorting());
                BufferBuilder.RenderedBuffer data = entry.getValue().end();
                if (data.isEmpty()) { data.release(); continue; }
                VertexBuffer vertices = new VertexBuffer(VertexBuffer.Usage.STATIC);
                parts.add(new Part(entry.getKey(), vertices));
                vertices.bind();
                vertices.upload(data);
            }
            return new Mesh(parts);
        } catch (RuntimeException | Error failure) {
            for (Part part : parts) part.vertices.close();
            throw failure;
        } finally {
            VertexBuffer.unbind();
            for (BufferBuilder builder : capture.builders.values()) builder.discard();
        }
    }

    public static void clear() {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(StaticGuiItemCache::clear); return; }
        for (Mesh mesh : CACHE.values()) mesh.close();
        CACHE.clear();
    }

    private record Key(ModelItem item, ItemGuiRenderContext.Purpose purpose, BakedModel baked) {}
    private record Part(RenderType type, VertexBuffer vertices) {}
    private record Mesh(List<Part> parts) {
        void close() { for (Part part : parts) part.vertices.close(); }
    }
    private static final class Capture implements MultiBufferSource {
        final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        @Override public VertexConsumer getBuffer(RenderType type) {
            return builders.computeIfAbsent(type, key -> {
                BufferBuilder builder = new BufferBuilder(4096);
                builder.begin(key.mode(), key.format());
                return builder;
            });
        }
    }
}
