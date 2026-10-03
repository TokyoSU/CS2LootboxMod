package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shares the exact evaluated rigid mesh across hands, dropped stacks, frames and previews.
 * World rendering replays it into Minecraft's buffers, preserving normal transforms,
 * lighting, glint, shader integrations and translucent sorting without forcing world flushes.
 */
public final class CaseGeometryCache {
    private static final Map<Key, Mesh> CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private CaseGeometryCache() {}

    static LocalGeometry get(BakedGeoModel model, HeldCaseMeshCache.GeometryWriter writer) {
        Key key = new Key(model);
        Mesh mesh = CACHE.get(key);
        if (mesh == null || !mesh.pose.matches(model)) {
            LocalGeometry vertices = new LocalGeometry();
            writer.write(new PoseStack(), vertices);
            vertices.finish();
            mesh = new Mesh(new CaseMeshPose(model), vertices);
            CACHE.put(key, mesh);
            if (CACHE.size() > 16) {
                var oldest = CACHE.entrySet().iterator();
                oldest.next(); oldest.remove();
            }
        }
        return mesh.vertices;
    }

    static boolean supports(PoseStack pose) {
        // Negative/nonuniform scales in Minecraft can produce unbounded normals.
        // Preserve its original vertex path rather than amplifying round-off there.
        var n = pose.last().normal();
        for (int column = 0; column < 3; column++) for (int row = 0; row < 3; row++) {
            float value = n.get(column, row);
            if (!Float.isFinite(value) || Math.abs(value) > 64) return false;
        }
        return true;
    }

    static boolean supports(List<GeoBone> bones) {
        for (int i = 0; i < bones.size(); i++) {
            GeoBone bone = bones.get(i);
            if (bone.isTrackingMatrices()) return false;
            // The same normal-transform constraint also applies inside the hierarchy.
            if (!safeScale(bone.getScaleX()) || !safeScale(bone.getScaleY()) || !safeScale(bone.getScaleZ())) return false;
            // GeckoLib's flat-cube normal correction operates in the final coordinate
            // space, so it cannot be factored into a cached local normal.
            for (var cube : bone.getCubes()) {
                if (cube.size().x == 0 || cube.size().y == 0 || cube.size().z == 0) return false;
            }
            if (!supports(bone.getChildBones())) return false;
        }
        return true;
    }

    private static boolean safeScale(float value) {
        return Float.isFinite(value) && value >= 1F / 64 && value <= 64;
    }

    public static void clear() { CACHE.clear(); }
    private record Mesh(CaseMeshPose pose, LocalGeometry vertices) {}
    private record Key(BakedGeoModel model) {
        @Override public boolean equals(Object other) { return other instanceof Key key && model == key.model; }
        @Override public int hashCode() { return System.identityHashCode(model); }
    }
}
