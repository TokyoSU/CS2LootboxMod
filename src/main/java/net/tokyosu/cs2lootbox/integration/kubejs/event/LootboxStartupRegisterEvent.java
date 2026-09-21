package net.tokyosu.cs2lootbox.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinitionBuilder;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import net.tokyosu.cs2lootbox.registry.LootboxRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Startup-only KubeJS event used to define or edit complete lootbox definitions
 * before the Minecraft item registry closes.
 */
public final class LootboxStartupRegisterEvent extends StartupEventJS {
    @Info(value = "Registers a complete CS2 lootbox and its matching key item.", params = {
            @Param(name = "id", value = "Lootbox definition id, for example kubejs:revolution"),
            @Param(name = "config", value = "Builder callback used to configure model, texture, animation, sounds, loot and GUI transform")
    })
    public void addCrate(@NotNull String id, @NotNull Consumer<LootboxDefinitionBuilder> config) {
        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(id);
        Objects.requireNonNull(config, "config").accept(builder);
        LootboxRegistrationService.register(builder);
    }

    @Info(value = "Alias of addCrate(). Registers a complete CS2 case and its matching key item.", params = {
            @Param(name = "id", value = "Lootbox definition id, for example kubejs:revolution"),
            @Param(name = "config", value = "Builder callback used to configure the case")
    })
    public void addCase(@NotNull String id, @NotNull Consumer<LootboxDefinitionBuilder> config) {
        addCrate(id, config);
    }

    @Info(value = "Edits an already-created lootbox, including the mod's built-in default case. Existing values are copied first, so only fields changed by the callback are replaced.", params = {
            @Param(name = "id", value = "Existing lootbox definition id. Use a full id such as cs2lootbox:aus2025_promo_de_ancient for built-in cases."),
            @Param(name = "config", value = "Builder callback used to edit the copied definition. Use clearLoot()/clearLegendary() when replacing existing reward tables.")
    })
    public void changeCase(@NotNull String id, @NotNull Consumer<LootboxDefinitionBuilder> config) {
        ResourceLocation resourceId = parseId(id);
        LootboxDefinition existing = LootboxRegistry.get(resourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot change unknown lootbox '" + resourceId + "'. " +
                        "The case must be registered before changeCase() is called."
                ));

        LootboxDefinitionBuilder builder = new LootboxDefinitionBuilder(existing);
        Objects.requireNonNull(config, "config").accept(builder);
        LootboxRegistrationService.change(builder);
    }

    private static @NotNull ResourceLocation parseId(@NotNull String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Lootbox id cannot be empty");
        }

        String normalized = value.trim();
        int separator = normalized.indexOf(':');
        if (separator >= 0) {
            return ResourceLocation.fromNamespaceAndPath(
                    normalized.substring(0, separator),
                    normalized.substring(separator + 1)
            );
        }

        return ResourceLocation.fromNamespaceAndPath("kubejs", normalized);
    }
}
