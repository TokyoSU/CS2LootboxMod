package net.tokyosu.cs2lootbox.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Global options that must be available before a world-specific SERVER config
 * is loaded.
 */
public final class CS2LootboxCommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SILENCE_GECKOMESH_INFO_LOGS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("logging");

        SILENCE_GECKOMESH_INFO_LOGS = builder
                .comment(
                        "Suppress GeckoMesh INFO/DEBUG logging.",
                        "GeckoMesh 1.1 currently logs every PolyMesh face/vertex at INFO,",
                        "which can generate tens of thousands of debug.log lines.",
                        "WARN and ERROR messages remain visible.")
                .define("silenceGeckoMeshInfoLogs", true);

        builder.pop();

        SPEC = builder.build();
    }
}
