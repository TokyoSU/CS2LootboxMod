package net.tokyosu.cs2lootbox.api.music;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import java.util.List;

/** Startup-defined music library. Tracks have stable IDs so saved discs survive reordering. */
public record MusicKitDefinition(ResourceLocation id, String name, ResourceLocation image,
                                 int imageWidth, int imageHeight, float imageScale,
                                 ResourceLocation itemTexture, LootRarityGrade grade, LootboxDefinition display, List<Track> tracks) {
    public MusicKitDefinition { tracks = List.copyOf(tracks); }

    public record Track(String id, String title, ResourceLocation sound, int lengthInTicks) {
        public double seconds() { return lengthInTicks / 20.0; }
        public Component titleComponent() { return text(title); }
    }

    public ResourceLocation itemId(int index) {
        // All tracks have stable variant IDs. The public kit ID is an additional default disc.
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/track/" + tracks.get(index).id());
    }

    public Component nameComponent() { return text(name); }

    /** Accept the same literal / translation-key convention as case tooltips. */
    public static Component text(String value) {
        return value.indexOf('.') < 0 ? Component.literal(value) : Component.translatable(value);
    }
}
