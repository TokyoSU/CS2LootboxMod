package net.tokyosu.cs2lootbox.registry;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime registry for KubeJS/Java-defined lootboxes.
 *
 * Definitions are immutable snapshots. Editing an existing case therefore
 * replaces the snapshot while preserving its id and declaration order.
 */
public final class LootboxRegistry {
    private static final Map<ResourceLocation, LootboxDefinition> DEFINITIONS = new LinkedHashMap<>();

    private LootboxRegistry() {
    }

    public static synchronized void register(@NotNull LootboxDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        LootboxDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate lootbox definition id: " + definition.id());
        }
    }

    /**
     * Replaces an already-registered definition without changing registry order.
     */
    public static synchronized void replace(@NotNull LootboxDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        ResourceLocation id = definition.id();
        if (!DEFINITIONS.containsKey(id)) {
            throw new IllegalArgumentException("Cannot change unknown lootbox definition id: " + id);
        }
        DEFINITIONS.put(id, definition);
    }

    public static synchronized @NotNull Optional<LootboxDefinition> get(@NotNull ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(id, "id")));
    }

    public static synchronized @NotNull LootboxDefinition getOrDefault(@NotNull ResourceLocation id) {
        LootboxDefinition definition = DEFINITIONS.get(Objects.requireNonNull(id, "id"));
        if (definition != null) {
            return definition;
        }

        definition = DEFINITIONS.get(BuiltInLootboxes.DEFAULT_ID);
        if (definition == null) {
            throw new IllegalStateException("Default lootbox definition has not been registered");
        }
        return definition;
    }

    public static synchronized @NotNull Collection<LootboxDefinition> values() {
        return List.copyOf(DEFINITIONS.values());
    }
}
