package net.tokyosu.cs2lootbox.client.renderer.skinning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Loads and validates the optional {@code cs2_skinning} extension. */
public final class SkinnedGeoModelLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, SkinnedGeoModelData> CACHE = new ConcurrentHashMap<>();
    private static ResourceManager cachedResourceManager;

    private SkinnedGeoModelLoader() {
    }

    public static @NotNull SkinnedGeoModelData get(@NotNull ResourceLocation modelResource) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager != cachedResourceManager) {
            CACHE.clear();
            cachedResourceManager = resourceManager;
        }
        return CACHE.computeIfAbsent(modelResource, location -> load(resourceManager, location));
    }

    /** Useful for resource-pack reloads and development hot reloads. */
    public static void clear() {
        CACHE.clear();
    }

    private static @NotNull SkinnedGeoModelData load(
            @NotNull ResourceManager resourceManager,
            @NotNull ResourceLocation modelResource) {
        try {
            Resource resource = resourceManager.getResource(modelResource).orElse(null);
            if (resource == null) {
                return SkinnedGeoModelData.EMPTY;
            }

            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    return SkinnedGeoModelData.EMPTY;
                }

                JsonObject root = parsed.getAsJsonObject();
                JsonArray geometryArray = array(root, "minecraft:geometry");
                if (geometryArray == null || geometryArray.size() == 0) {
                    return SkinnedGeoModelData.EMPTY;
                }

                JsonObject geometry = geometryArray.get(0).isJsonObject()
                        ? geometryArray.get(0).getAsJsonObject()
                        : null;
                if (geometry == null || !geometry.has("cs2_skinning") || !geometry.get("cs2_skinning").isJsonObject()) {
                    return SkinnedGeoModelData.EMPTY;
                }

                JsonObject skinning = geometry.getAsJsonObject("cs2_skinning");
                int formatVersion = skinning.has("format_version") ? skinning.get("format_version").getAsInt() : 0;
                if (formatVersion != 1) {
                    LOGGER.warn("Unsupported CS2 skinning format {} in {}", formatVersion, modelResource);
                    return SkinnedGeoModelData.EMPTY;
                }

                JsonArray meshesJson = array(skinning, "meshes");
                if (meshesJson == null || meshesJson.size() == 0) {
                    return SkinnedGeoModelData.EMPTY;
                }

                List<SkinnedGeoModelData.SkinnedMesh> meshes = new ArrayList<>();
                for (JsonElement element : meshesJson) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    SkinnedGeoModelData.SkinnedMesh mesh = parseMesh(element.getAsJsonObject(), modelResource);
                    if (mesh != null) {
                        meshes.add(mesh);
                    }
                }

                if (!meshes.isEmpty()) {
                    int vertices = meshes.stream().mapToInt(SkinnedGeoModelData.SkinnedMesh::vertexCount).sum();
                    int triangles = meshes.stream().mapToInt(SkinnedGeoModelData.SkinnedMesh::triangleCount).sum();
                    LOGGER.debug("Loaded CS2 weighted skinning from {}: {} mesh(es), {} vertices, {} triangles",
                            modelResource, meshes.size(), vertices, triangles);
                }
                return meshes.isEmpty() ? SkinnedGeoModelData.EMPTY : new SkinnedGeoModelData(meshes);
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to load CS2 weighted skinning from {}", modelResource, exception);
            return SkinnedGeoModelData.EMPTY;
        }
    }

    private static SkinnedGeoModelData.SkinnedMesh parseMesh(JsonObject json, ResourceLocation modelResource) {
        String name = json.has("name") ? json.get("name").getAsString() : "weighted_mesh";
        String[] bones = stringArray(array(json, "bones"));
        float[] positions = floatArray(array(json, "positions"));
        float[] uvs = floatArray(array(json, "uvs"));
        int[] joints = intArray(array(json, "joints"));
        float[] weights = floatArray(array(json, "weights"));
        int[] indices = intArray(array(json, "indices"));

        int vertexCount = positions.length / 3;
        boolean valid = positions.length > 0
                && positions.length % 3 == 0
                && uvs.length == vertexCount * 2
                && joints.length == vertexCount * 4
                && weights.length == vertexCount * 4
                && indices.length > 0
                && indices.length % 3 == 0
                && bones.length > 0;

        if (!valid) {
            LOGGER.warn("Ignoring malformed CS2 weighted mesh '{}' in {}", name, modelResource);
            return null;
        }

        for (int index : indices) {
            if (index < 0 || index >= vertexCount) {
                LOGGER.warn("Ignoring CS2 weighted mesh '{}' in {} because an index ({}) is outside 0..{}",
                        name, modelResource, index, vertexCount - 1);
                return null;
            }
        }

        // Normalize weights once when the resource is loaded. This makes the
        // renderer tolerant of tiny JSON rounding error while keeping glTF's
        // original four-influence blend.
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int base = vertex * 4;
            float sum = 0.0F;
            for (int influence = 0; influence < 4; influence++) {
                float weight = weights[base + influence];
                if (!Float.isFinite(weight) || weight < 0.0F) {
                    weight = 0.0F;
                    weights[base + influence] = 0.0F;
                }
                int joint = joints[base + influence];
                if (joint < 0 || joint >= bones.length) {
                    weights[base + influence] = 0.0F;
                    joints[base + influence] = 0;
                    continue;
                }
                sum += weight;
            }
            if (sum > 1.0E-8F) {
                for (int influence = 0; influence < 4; influence++) {
                    weights[base + influence] /= sum;
                }
            }
        }

        return new SkinnedGeoModelData.SkinnedMesh(name, bones, positions, uvs, joints, weights, indices);
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String[] stringArray(JsonArray array) {
        if (array == null) {
            return new String[0];
        }
        String[] values = new String[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsString();
        }
        return values;
    }

    private static float[] floatArray(JsonArray array) {
        if (array == null) {
            return new float[0];
        }
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    private static int[] intArray(JsonArray array) {
        if (array == null) {
            return new int[0];
        }
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsInt();
        }
        return values;
    }
}
