package net.tokyosu.cs2lootbox.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resource-pack availability, independent of Minecraft's client and item registration. */
public final class MusicKitResources {
    private MusicKitResources() { }

    public static Set<ResourceLocation> findAvailable(ResourceManager resources, List<MusicKitDefinition> kits) {
        Map<ResourceLocation, List<SoundReference>> sounds = new HashMap<>();
        Set<String> namespaces = new HashSet<>(resources.getNamespaces());
        for (var kit : kits) for (var track : kit.tracks()) namespaces.add(track.sound().getNamespace());
        for (var namespace : namespaces) {
            for (var resource : resources.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "sounds.json"))) {
                try (var reader = resource.openAsReader()) {
                    var events = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : events.entrySet()) {
                        var definition = entry.getValue().getAsJsonObject();
                        var id = ResourceLocation.fromNamespaceAndPath(namespace, entry.getKey());
                        if (definition.has("replace") && definition.get("replace").getAsBoolean()) sounds.remove(id);
                        var references = sounds.computeIfAbsent(id, key -> new ArrayList<>());
                        if (definition.has("sounds")) {
                            for (var sound : definition.getAsJsonArray("sounds")) references.add(reference(sound));
                        }
                    }
                } catch (Exception ignored) {
                    // A malformed optional pack cannot enable a kit with unusable resources.
                }
            }
        }
        Set<ResourceLocation> available = new HashSet<>();
        Map<ResourceLocation, Boolean> audio = new HashMap<>();
        for (var kit : kits) {
            if (kit.image() != null && resources.getResource(kit.image()).isEmpty()) continue;
            var display = kit.display();
            if (display != null && (resources.getResource(display.model()).isEmpty()
                    || resources.getResource(display.texture()).isEmpty()
                    || resources.getResource(display.animation()).isEmpty())) continue;
            boolean complete = true;
            for (var track : kit.tracks()) {
                if (!audio.computeIfAbsent(track.sound(), id -> hasAudio(id, sounds, resources, new HashSet<>()))) {
                    complete = false;
                    break;
                }
            }
            if (complete) available.add(kit.id());
        }
        return Set.copyOf(available);
    }

    private static SoundReference reference(JsonElement element) {
        if (element.isJsonPrimitive()) return new SoundReference(ResourceLocation.parse(element.getAsString()), false);
        var sound = element.getAsJsonObject();
        return new SoundReference(ResourceLocation.parse(sound.get("name").getAsString()),
                sound.has("type") && sound.get("type").getAsString().equals("event"));
    }

    private static boolean hasAudio(ResourceLocation id, Map<ResourceLocation, List<SoundReference>> sounds,
                                    ResourceManager resources, Set<ResourceLocation> visited) {
        if (!visited.add(id)) return false;
        try {
            for (var reference : sounds.getOrDefault(id, List.of())) {
                if (reference.event()) {
                    if (hasAudio(reference.id(), sounds, resources, visited)) return true;
                } else if (resources.getResource(ResourceLocation.fromNamespaceAndPath(reference.id().getNamespace(),
                        "sounds/" + reference.id().getPath() + ".ogg")).isPresent()) return true;
            }
            return false;
        } finally { visited.remove(id); }
    }

    private record SoundReference(ResourceLocation id, boolean event) { }
}
