package net.tokyosu.cs2lootbox.registry;

import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.integration.kubejs.builder.LootboxCaseItemBuilder;
import net.tokyosu.cs2lootbox.integration.kubejs.builder.LootboxKeyItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
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
    private static final java.util.Map<ResourceLocation, net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder.Definition> MODEL_ITEMS = new java.util.LinkedHashMap<>();
    private static boolean finalized;

    public static synchronized void registerItem(net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder builder) {
        if (finalized) throw new IllegalStateException("Model items must be added during startup registration");
        var definition = builder.build();
        var id = definition.display().caseItemId();
        if (MODEL_ITEMS.putIfAbsent(id, definition) != null)
            throw new IllegalArgumentException("Duplicate model item: " + id);
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
        finalized = true;
    }
}
