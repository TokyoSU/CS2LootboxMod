package net.tokyosu.cs2lootbox.api.music;

/** Legacy paths resolve to the renamed shared meshes without duplicating assets. */
public final class MusicKitModelResources {
    private MusicKitModelResources() { }

    public static String canonical(String resource) {
        if (resource == null) return null;
        return switch (resource) {
            case "cs2lootbox:geo/music_kit_treeadams_benbromfield_01.geo.json" ->
                    "cs2lootbox:geo/music_kit_standard_1024x1024.geo.json";
            case "cs2lootbox:geo/music_kit_lenniemoore_01.geo.json" ->
                    "cs2lootbox:geo/music_kit_standard_1024x512.geo.json";
            case "cs2lootbox:geo/music_kit_adambeyer_01.geo.json" ->
                    "cs2lootbox:geo/music_kit_cs2_2048x1024.geo.json";
            case "cs2lootbox:geo/music_kit_valve_01.geo.json" ->
                    "cs2lootbox:geo/music_kit_valve_csgo_2048x1024.geo.json";
            default -> resource;
        };
    }
}
