package net.tokyosu.cs2lootbox.api.music;

import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.apocalypselib.utils.ResourceUtils;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;
import net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

/** Register with CS2LootboxEvents.register(event => event.addMusicKit(id, kit => ...)). */
public final class MusicKitDefinitionBuilder {
    private final ResourceLocation id;
    private final ModelItemDefinitionBuilder display;
    private boolean hasModel, hasTexture;
    private String name;
    private ResourceLocation image;
    private ResourceLocation itemTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "item/music_disc_13");
    private int imageWidth = 512, imageHeight = 512;
    private float imageScale = 1;
    private LootRarityGrade grade = LootRarityGrade.MIL_SPEC;
    private final ArrayList<MusicKitDefinition.Track> tracks = new ArrayList<>();
    private final HashSet<String> trackIds = new HashSet<>();

    public MusicKitDefinitionBuilder(String id) {
        this.id = ResourceUtils.parseRequired(id, "kubejs");
        display = new ModelItemDefinitionBuilder(this.id).stackSize(1);
        name = "item." + this.id.getNamespace() + "." + this.id.getPath().replace('/', '.');
    }

    public MusicKitDefinitionBuilder(MusicKitDefinition existing) {
        id = existing.id(); name = existing.name(); image = existing.image();
        imageWidth = existing.imageWidth(); imageHeight = existing.imageHeight(); imageScale = existing.imageScale();
        itemTexture = existing.itemTexture(); grade = existing.grade();
        display = existing.display() == null ? new ModelItemDefinitionBuilder(id).stackSize(1)
                : new ModelItemDefinitionBuilder(new ModelItemDefinitionBuilder.Definition(existing.display(), grade));
        hasModel = hasTexture = existing.display() != null;
        tracks.addAll(existing.tracks());
        for (var track : tracks) trackIds.add(track.id());
    }

    public MusicKitDefinitionBuilder name(String value) {
        name = required(value, "Music kit name"); return this;
    }

    public MusicKitDefinitionBuilder image(String value) { return image(value, 512, 512); }

    public MusicKitDefinitionBuilder image(String value, int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
        image = ResourceUtils.parseRequired(value, id.getNamespace());
        if (!image.getPath().startsWith("textures/") || !image.getPath().endsWith(".png"))
            throw new IllegalArgumentException("Music kit image requires a full textures/...png resource path");
        imageWidth = width; imageHeight = height;
        itemTexture = ResourceLocation.fromNamespaceAndPath(image.getNamespace(),
                image.getPath().substring(9, image.getPath().length() - 4));
        return this;
    }

    public MusicKitDefinitionBuilder imageScale(float value) {
        if (!Float.isFinite(value) || value <= 0 || value > 2)
            throw new IllegalArgumentException("Image scale must be greater than 0 and at most 2");
        imageScale = value; return this;
    }

    public MusicKitDefinitionBuilder itemTexture(String value) {
        itemTexture = ResourceUtils.parseRequired(value, id.getNamespace()); return this;
    }

    public MusicKitDefinitionBuilder rarityGrade(String value) { grade = LootRarityGrade.fromId(value); return this; }
    @HideFromJS
    public MusicKitDefinitionBuilder rarityGrade(LootRarityGrade value) { grade = Objects.requireNonNull(value); return this; }

    public MusicKitDefinitionBuilder model(String value) { display.model(value); hasModel = true; return this; }
    public MusicKitDefinitionBuilder texture(String value) { display.texture(value); hasTexture = true; return this; }
    public MusicKitDefinitionBuilder firstPersonRight(float x, float y, float z, float rx, float ry, float rz, float s) { display.firstPersonRight(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder firstPersonLeft(float x, float y, float z, float rx, float ry, float rz, float s) { display.firstPersonLeft(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder thirdPersonRight(float x, float y, float z, float rx, float ry, float rz, float s) { display.thirdPersonRight(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder thirdPersonLeft(float x, float y, float z, float rx, float ry, float rz, float s) { display.thirdPersonLeft(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder gui(float x, float y, float z, float rx, float ry, float rz, float s) { display.gui(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder ground(float x, float y, float z, float rx, float ry, float rz, float s) { display.ground(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder fixed(float x, float y, float z, float rx, float ry, float rz, float s) { display.fixed(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder guiCarousel(float x, float y, float z, float rx, float ry, float rz, float s) { display.guiCarousel(x, y, z, rx, ry, rz, s); return this; }
    public MusicKitDefinitionBuilder guiReveal(float x, float y, float z, float rx, float ry, float rz, float s) { display.guiReveal(x, y, z, rx, ry, rz, s); return this; }

    public MusicKitDefinitionBuilder clearTracks() { tracks.clear(); trackIds.clear(); return this; }
    public MusicKitDefinitionBuilder removeTrack(String id) {
        tracks.removeIf(track -> track.id().equals(id)); trackIds.remove(id); return this;
    }

    /** Sound is an event ID from sounds.json; duration is the actual audio duration in seconds. */
    public MusicKitDefinitionBuilder track(String trackId, String title, String sound, double seconds) {
        if (trackId == null || !trackId.matches("[a-z0-9_/-]+"))
            throw new IllegalArgumentException("Track IDs must use lowercase letters, digits, _, - or /");
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 86400)
            throw new IllegalArgumentException("Track duration must be greater than 0 and at most 86400 seconds");
        var track = new MusicKitDefinition.Track(trackId, required(title, "Track title"),
                ResourceUtils.parseRequired(sound, id.getNamespace()), Math.max(1, (int) Math.ceil(seconds * 20)));
        if (!trackIds.add(trackId)) throw new IllegalArgumentException("Duplicate music track: " + trackId);
        tracks.add(track); return this;
    }

    public MusicKitDefinition build() {
        if (tracks.isEmpty()) throw new IllegalStateException("Music kit " + id + " needs at least one track()");
        if (hasModel != hasTexture) throw new IllegalStateException("Music kit model() and texture() must be supplied together");
        return new MusicKitDefinition(id, name, image, imageWidth, imageHeight, imageScale, itemTexture, grade,
                hasModel ? display.build().display() : null, tracks);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " cannot be empty");
        return value.trim();
    }
}
