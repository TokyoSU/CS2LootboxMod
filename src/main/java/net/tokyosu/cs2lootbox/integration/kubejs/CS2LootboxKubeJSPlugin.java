package net.tokyosu.cs2lootbox.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import net.tokyosu.cs2lootbox.integration.kubejs.event.LootboxStartupRegisterEvent;
import net.tokyosu.cs2lootbox.registry.BuiltInLootBoxes;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import org.jetbrains.annotations.NotNull;

/**
 * KubeJS integration entry point.
 *
 * KubeJS 6 calls initStartup after startup scripts are loaded but before
 * registry objects are created. Definitions are registered/edited first, then
 * their final case/key ItemBuilders are created once the event has completed.
 */
public final class CS2LootboxKubeJSPlugin extends KubeJSPlugin {
    public static final @NotNull EventGroup EVENTS = EventGroup.of("CS2LootboxEvents");
    public static final @NotNull EventHandler REGISTER = EVENTS.startup(
            "register",
            () -> LootboxStartupRegisterEvent.class
    );

    @Override
    public void registerEvents() {
        EVENTS.register();
    }

    @Override
    public void registerClasses(@NotNull ScriptType type, @NotNull ClassFilter filter) {
        filter.allow("net.tokyosu.cs2lootbox.api.lootbox");
        filter.allow("net.tokyosu.cs2lootbox.integration.kubejs.event");
    }

    @Override
    public void initStartup() {
        BuiltInLootBoxes.register();
        REGISTER.post(new LootboxStartupRegisterEvent());
        LootboxRegistrationService.finalizeItemRegistrations();
    }
}
