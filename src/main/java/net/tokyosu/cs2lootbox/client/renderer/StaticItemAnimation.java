package net.tokyosu.cs2lootbox.client.renderer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Conservative eligibility check: expressions, keyframes and animation events use the live renderer. */
final class StaticItemAnimation {
    private static final Map<Key, Boolean> CACHE = new HashMap<>();

    private StaticItemAnimation() {}

    static boolean isStatic(ResourceLocation resource, String animation) {
        return CACHE.computeIfAbsent(new Key(resource, animation), key -> {
            var found = Minecraft.getInstance().getResourceManager().getResource(key.resource());
            if (found.isEmpty()) return false;
            try (var reader = new InputStreamReader(found.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                return animations != null && isStaticClip(animations.get(key.animation()));
            } catch (Exception ignored) {
                // GeckoLib remains responsible for reporting invalid animation resources.
                return false;
            }
        });
    }

    static boolean isStaticClip(JsonElement element) {
        if (element == null || !element.isJsonObject()) return false;
        JsonObject clip = element.getAsJsonObject();
        for (String key : clip.keySet()) {
            if (!key.equals("loop") && !key.equals("animation_length") && !key.equals("bones")) return false;
        }
        if (!clip.has("bones") || !clip.get("bones").isJsonObject()) return false;
        for (JsonElement bone : clip.getAsJsonObject("bones").asMap().values()) {
            if (!bone.isJsonObject()) return false;
            for (var channel : bone.getAsJsonObject().entrySet()) {
                if (!channel.getKey().equals("rotation") && !channel.getKey().equals("position")
                        && !channel.getKey().equals("scale")) return false;
                JsonElement vector = channel.getValue();
                if (vector.isJsonObject()) {
                    JsonObject object = vector.getAsJsonObject();
                    if (object.size() != 1 || !object.has("vector")) return false;
                    vector = object.get("vector");
                }
                if (!vector.isJsonArray() || vector.getAsJsonArray().size() != 3) return false;
                for (JsonElement component : vector.getAsJsonArray()) {
                    if (!component.isJsonPrimitive() || !component.getAsJsonPrimitive().isNumber()
                            || !Double.isFinite(component.getAsDouble())) return false;
                }
            }
        }
        return true;
    }

    static void clear() { CACHE.clear(); }

    private record Key(ResourceLocation resource, String animation) {}
}
