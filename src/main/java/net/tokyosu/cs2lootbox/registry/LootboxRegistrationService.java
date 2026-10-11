package net.tokyosu.cs2lootbox.registry;

import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinitionBuilder;
import net.tokyosu.cs2lootbox.integration.kubejs.builder.MusicKitItemBuilder;
import net.tokyosu.cs2lootbox.integration.kubejs.builder.LootboxCaseItemBuilder;
import net.tokyosu.cs2lootbox.integration.kubejs.builder.LootboxKeyItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Single registration path used by built-in content and KubeJS content.
 *
 * Definition registration is intentionally separated from Minecraft ItemBuilder
 * creation. KubeJS may call changeCase() during the startup event, including on
 * built-in cases, so item builders are finalized only after all scripts have
 * finished editing the definitions.
 */
public final class LootboxRegistrationService {
    private static final Set<ResourceLocation> FINALIZED_ITEM_BUILDERS = new HashSet<>();
    private static final Map<ResourceLocation, ModelItemDefinitionBuilder.Definition> MODEL_ITEMS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, MusicKitDefinition> MUSIC_KITS = new LinkedHashMap<>();
    private static boolean finalized;
    private static final Set<ResourceLocation> MUSIC_TRACK_SOUNDS = new HashSet<>();

    public static synchronized boolean isMusicKitSound(ResourceLocation sound) {
        return MUSIC_TRACK_SOUNDS.contains(sound);
    }

    public static synchronized void registerMusicKit(MusicKitDefinitionBuilder builder) {
        if (finalized) throw new IllegalStateException("Music kits must be added during startup registration");
        var definition = builder.build();
        if (MUSIC_KITS.putIfAbsent(definition.id(), definition) != null)
            throw new IllegalArgumentException("Duplicate music kit: " + definition.id());
    }

    public static synchronized MusicKitDefinition musicKit(ResourceLocation id) {
        var kit = MUSIC_KITS.get(id);
        if (kit == null) throw new IllegalArgumentException("Unknown music kit: " + id);
        return kit;
    }

    public static synchronized List<MusicKitDefinition> musicKits() {
        return List.copyOf(MUSIC_KITS.values());
    }

    public static synchronized void changeMusicKit(MusicKitDefinitionBuilder builder) {
        if (finalized) throw new IllegalStateException("Music kits must be changed during startup registration");
        var definition = builder.build();
        musicKit(definition.id());
        MUSIC_KITS.put(definition.id(), definition);
    }

    public static synchronized void registerItem(ModelItemDefinitionBuilder builder) {
        if (finalized) throw new IllegalStateException("Model items must be added during startup registration");
        var definition = builder.build();
        var id = definition.display().caseItemId();
        if (MODEL_ITEMS.putIfAbsent(id, definition) != null)
            throw new IllegalArgumentException("Duplicate model item: " + id);
    }

    /** Cases and keys first, then standalone rewards, preserving definition order. */
    public static synchronized List<ResourceLocation> creativeItemIds() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (LootboxDefinition definition : LootboxRegistry.values()) {
            ids.add(definition.caseItemId());
            if (definition.requiresKey()) ids.add(definition.keyItemId());
        }
        ids.addAll(MODEL_ITEMS.keySet());
        ids.addAll(MUSIC_KITS.keySet());
        return List.copyOf(ids);
    }
    private LootboxRegistrationService() {
    }

    public static synchronized @NotNull LootboxDefinition register(@NotNull LootboxDefinitionBuilder builder) {
        if (finalized) {
            throw new IllegalStateException("Lootbox definitions can only be added before item registration is finalized");
        }

        LootboxDefinition definition = Objects.requireNonNull(builder, "builder").build();
        LootboxRegistry.register(definition);
        return definition;
    }

    public static synchronized @NotNull LootboxDefinition change(@NotNull LootboxDefinitionBuilder builder) {
        if (finalized) {
            throw new IllegalStateException("Lootbox definitions can only be changed during CS2LootboxEvents.register startup processing");
        }

        LootboxDefinition definition = Objects.requireNonNull(builder, "builder").build();
        LootboxRegistry.replace(definition);
        return definition;
    }

    /**
     * Adds the final ItemBuilders after built-in and KubeJS definitions have all
     * been created/edited. This is deliberately called once after the startup
     * event has finished dispatching.
     */
    public static synchronized void finalizeItemRegistrations() {
        if (finalized) {
            return;
        }

        Set<ResourceLocation> itemIds = new HashSet<>(MODEL_ITEMS.keySet());
        for (LootboxDefinition definition : LootboxRegistry.values()) {
            if (!itemIds.add(definition.caseItemId())
                    || (definition.requiresKey() && !itemIds.add(definition.keyItemId())))
                throw new IllegalArgumentException("Duplicate case, key or model item id in " + definition.id());
        }

        for (var kit : MUSIC_KITS.values()) {
            if (!itemIds.add(kit.id())) throw new IllegalArgumentException("Duplicate music kit item id: " + kit.id());
            for (int i = 0; i < kit.tracks().size(); i++) {
                if (!itemIds.add(kit.itemId(i))) throw new IllegalArgumentException("Duplicate music track item id: " + kit.itemId(i));
            }
        }

        for (LootboxDefinition definition : LootboxRegistry.values()) {
            if (!FINALIZED_ITEM_BUILDERS.add(definition.id())) {
                continue;
            }
            RegistryInfo.ITEM.addBuilder(new LootboxCaseItemBuilder(definition));
            if (definition.requiresKey()) {
                RegistryInfo.ITEM.addBuilder(new LootboxKeyItemBuilder(definition));
            }
        }

        for (var definition : MODEL_ITEMS.values()) {
            var id = definition.display().caseItemId();
            if (!FINALIZED_ITEM_BUILDERS.add(id))
                throw new IllegalArgumentException("Duplicate item id: " + id);
            RegistryInfo.ITEM.addBuilder(new net.tokyosu.cs2lootbox.integration.kubejs.builder.ModelItemBuilder(definition));
        }
        for (var kit : MUSIC_KITS.values()) {
            RegistryInfo.ITEM.addBuilder(new MusicKitItemBuilder(kit.id(), kit, 0));
            for (int i = 0; i < kit.tracks().size(); i++) {
                RegistryInfo.ITEM.addBuilder(new MusicKitItemBuilder(kit.itemId(i), kit, i));
            }
        }
        MUSIC_TRACK_SOUNDS.clear();
        for (var kit : MUSIC_KITS.values()) {
            for (var track : kit.tracks()) MUSIC_TRACK_SOUNDS.add(track.sound());
        }
        finalized = true;
    }
}
