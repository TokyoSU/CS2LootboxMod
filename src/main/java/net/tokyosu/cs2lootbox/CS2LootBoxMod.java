package net.tokyosu.cs2lootbox;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.tokyosu.cs2lootbox.config.CS2LootboxClientConfig;
import net.tokyosu.cs2lootbox.config.CS2LootboxCommonConfig;
import net.tokyosu.cs2lootbox.config.CS2LootboxServerConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import net.tokyosu.cs2lootbox.registry.ModSounds;

@SuppressWarnings("removal")
@Mod(CS2LootBoxMod.MOD_ID)
public final class CS2LootBoxMod {
    public static final String MOD_ID = "cs2lootbox";

    public CS2LootBoxMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext modContext = ModLoadingContext.get();

        modContext.registerConfig(ModConfig.Type.CLIENT, CS2LootboxClientConfig.SPEC, "cs2lootbox-client.toml");
        modContext.registerConfig(ModConfig.Type.COMMON, CS2LootboxCommonConfig.SPEC, "cs2lootbox-common.toml");
        modContext.registerConfig(ModConfig.Type.SERVER, CS2LootboxServerConfig.SPEC, "cs2lootbox-server.toml");

        /*
         * GeckoMesh 1.1 currently emits every PolyMesh face and vertex at INFO.
         * Start quiet immediately so resource baking cannot fill debug.log
         * before Forge finishes loading our COMMON config. The config-loading
         * callback below restores INFO if the user explicitly opts out.
         */
        Configurator.setLevel("GeckoMesh", Level.WARN);

        modBus.addListener(this::onConfigLoaded);
        modBus.addListener(this::onConfigReloaded);

        ModSounds.register(modBus);
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CS2LootboxCommonConfig.SPEC) {
            applyLoggingConfig();
        }
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CS2LootboxCommonConfig.SPEC) {
            applyLoggingConfig();
        }
    }

    private static void applyLoggingConfig() {
        Configurator.setLevel("GeckoMesh", CS2LootboxCommonConfig.SILENCE_GECKOMESH_INFO_LOGS.get() ? Level.WARN : Level.INFO);
    }
}
