package net.tokyosu.cs2lootbox.registry.music;

import com.google.gson.Gson;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.music.MusicKitModelResources;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Kits use shared display geometry with a matching atlas layout. */
public final class BuiltInMusicKits {
    private BuiltInMusicKits() { }

    public static void register() {
        try (var input = BuiltInMusicKits.class.getResourceAsStream("/data/cs2lootbox/music_kits.json")) {
            if (input == null) throw new IllegalStateException("Missing built-in music kit catalogue");
            var entries = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), CatalogKit[].class);
            for (var entry : entries) {
                var kit = new MusicKitDefinitionBuilder("cs2lootbox:music_kit_" + entry.id())
                        .name("item.cs2lootbox.music_kit_" + entry.id())
                        .image("cs2lootbox:textures/gui/music/" + entry.id() + ".png", entry.imageWidth(), entry.imageHeight())
                        .itemTexture("cs2lootbox:gui/music/" + entry.id());
                String model = resolveModel(entry);
                if (model != null) {
                    configureModel(kit, model,
                            "cs2lootbox:textures/musickits/music_kit_" + entry.id() + ".png");
                }
                for (var track : entry.tracks()) {
                    kit.track(track.id(), "cs2lootbox.music.track." + track.id(),
                            "cs2lootbox:music." + entry.id() + "." + track.id(), track.seconds());
                }
                LootboxRegistrationService.registerMusicKit(kit);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to register built-in music kits", exception);
        }
    }

    private static String resolveModel(CatalogKit entry) {
        String model = MusicKitModelResources.canonical("cs2lootbox:geo/music_kit_" + entry.id() + ".geo.json");
        String filename = model.substring("cs2lootbox:".length());
        // Converted geometry belongs in the mod. An exact per-kit filename takes
        // priority over the catalogue's shared mesh and enables future conversions.
        if (BuiltInMusicKits.class.getResource("/assets/cs2lootbox/" + filename) != null) {
            return model;
        }
        // A shared model awaiting conversion can be selected as soon as it is
        // added to the mod, while the kit keeps its 2D fallback until then.
        String shared = MusicKitModelResources.canonical(entry.modelResource());
        if (shared != null && shared.startsWith("cs2lootbox:")
                && BuiltInMusicKits.class.getResource("/assets/cs2lootbox/"
                + shared.substring("cs2lootbox:".length())) != null) {
            return shared;
        }
        if (!entry.model()) return null;
        return shared == null || shared.isBlank()
                ? "cs2lootbox:geo/music_kit_standard_1024x1024.geo.json"
                : shared;
    }

    static void configureModel(MusicKitDefinitionBuilder kit, String model, String texture) {
        kit.model(model).texture(texture)
                .gui(0, -5.5F, 0, 0, 180, 0, 2.75F)
                .guiCarousel(0, -3.5F, 0, 0, 180, 0, 3.5F)
                .guiReveal(0, -6F, 0, 0, 180, 0, 4.5F)
                .firstPersonRight(0, -2F, 0, 0, 180, 0, 2.75F)
                .firstPersonLeft(0, -2F, 0, 0, 180, 0, 2.75F)
                .thirdPersonRight(0, 0, 0, 0, 180, 0, 2.75F)
                .thirdPersonLeft(0, 0, 0, 0, 180, 0, 2.75F)
                .ground(0, 0, 0, 0, 180, 0, 2.75F)
                .fixed(0, -4F, 0, 0, 180, 0, 2.75F);
    }

    private record CatalogKit(String id, int imageWidth, int imageHeight, boolean model,
                              String modelResource, List<CatalogTrack> tracks) { }
    private record CatalogTrack(String id, double seconds) { }
}
