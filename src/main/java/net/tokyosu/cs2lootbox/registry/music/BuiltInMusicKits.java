package net.tokyosu.cs2lootbox.registry.music;

import com.google.gson.Gson;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinitionBuilder;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Extracted kits share geometry only when their source mesh and atlas layout match. */
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
                if (entry.model()) {
                    configureModel(kit, "cs2lootbox:geo/music_kit_treeadams_benbromfield_01.geo.json",
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

    private record CatalogKit(String id, int imageWidth, int imageHeight, boolean model, List<CatalogTrack> tracks) { }
    private record CatalogTrack(String id, double seconds) { }
}
