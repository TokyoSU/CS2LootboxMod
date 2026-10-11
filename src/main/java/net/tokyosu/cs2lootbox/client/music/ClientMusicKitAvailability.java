package net.tokyosu.cs2lootbox.client.music;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import java.util.HashSet;
import java.util.Set;

/** Replaced atomically after each resource reload; never changes item registry IDs. */
public final class ClientMusicKitAvailability {
    private static volatile Set<ResourceLocation> kits = Set.of(), sounds = Set.of();
    private ClientMusicKitAvailability() { }
    public static boolean isAvailable(MusicKitDefinition kit) { return kits.contains(kit.id()); }
    public static boolean hasSound(ResourceLocation sound) { return sounds.contains(sound); }
    public static void update(Set<ResourceLocation> available) {
        var tracks = new HashSet<ResourceLocation>();
        for (var kit : LootboxRegistrationService.musicKits()) {
            if (available.contains(kit.id())) for (var track : kit.tracks()) tracks.add(track.sound());
        }
        sounds = Set.copyOf(tracks);
        kits = Set.copyOf(available);
    }
}
