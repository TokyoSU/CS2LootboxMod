package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Full-detail local geometry, uploaded once for a still case/model pose.
 * Only unsorted, unlit hand and preview passes use this: normals do not affect shading.
 * Camera/item transforms stay live in the draw matrix; light, tint and bone changes rebake.
 */
public final class HeldCaseMeshCache {
    private static final int MAX_ENTRIES = 16;
    private static final Map<Key, Mesh> CACHE = new LinkedHashMap<>(16, 0.75F, true);

    private HeldCaseMeshCache() {}

    @FunctionalInterface
    interface GeometryWriter { void write(PoseStack localPose, VertexConsumer vertices); }

    static void render(PoseStack pose, BakedGeoModel model, RenderType type,
                       int light, int overlay, float red, float green, float blue, float alpha,
                       GeometryWriter writer) {
        Key key = new Key(model, type);
        Attributes attributes = new Attributes(light, overlay, red, green, blue, alpha);
        Mesh mesh = CACHE.get(key);
        if (mesh == null || !mesh.attributes.equals(attributes) || !mesh.pose.matches(model)) {
            Mesh replacement = bake(model, type, attributes, writer);
            if (mesh != null) mesh.close();
            mesh = replacement;
            CACHE.put(key, mesh);
            if (CACHE.size() > MAX_ENTRIES) {
                var oldest = CACHE.entrySet().iterator();
                oldest.next().getValue().close();
                oldest.remove();
            }
        }
        if (mesh.vertices == null) return;
        type.setupRenderState();
        try {
            mesh.vertices.bind();
            Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
            mesh.vertices.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static Mesh bake(BakedGeoModel model, RenderType type, Attributes attributes, GeometryWriter writer) {
        BufferBuilder builder = new BufferBuilder(4096);
        VertexBuffer vertices = null;
        try {
            builder.begin(type.mode(), type.format());
            writer.write(new PoseStack(), builder);
            BufferBuilder.RenderedBuffer data = builder.end();
            if (data.isEmpty()) {
                data.release();
            } else {
                vertices = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vertices.bind();
                vertices.upload(data);
            }
            return new Mesh(vertices, new CaseMeshPose(model), attributes);
        } catch (RuntimeException | Error failure) {
            if (vertices != null) vertices.close();
            throw failure;
        } finally {
            VertexBuffer.unbind();
            builder.discard();
        }
    }

    public static void clear() {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(HeldCaseMeshCache::clear); return; }
        for (Mesh mesh : CACHE.values()) mesh.close();
        CACHE.clear();
        StaticItemAnimation.clear();
    }

    private record Key(BakedGeoModel model, RenderType type) {
        // BakedGeoModel/GeoBone equality is structural and can alias different resources.
        @Override public boolean equals(Object other) {
            return other instanceof Key key && model == key.model && type == key.type;
        }
        @Override public int hashCode() {
            return 31 * System.identityHashCode(model) + System.identityHashCode(type);
        }
    }
    private record Attributes(int light, int overlay, float red, float green, float blue, float alpha) {}
    private record Mesh(VertexBuffer vertices, CaseMeshPose pose, Attributes attributes) {
        void close() { if (vertices != null) vertices.close(); }
    }
}
