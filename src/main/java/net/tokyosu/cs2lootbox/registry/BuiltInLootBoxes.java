package net.tokyosu.cs2lootbox.registry;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.registry.crates.AgentDossier01;
import net.tokyosu.cs2lootbox.registry.crates.CSGOWeaponCase;

/**
 * Built-in CS:GO Weapon Case content.
 * The weapon/skin slots currently use distinct vanilla Minecraft items as
 * placeholders until the actual weapon registry exists. Their visible names,
 * rarity colors and case ordering already match the configured CS-style table.
 */
public final class BuiltInLootBoxes {
    /** Backwards-compatible alias used by existing UI/default lookup code. */
    public static final ResourceLocation DEFAULT_ID = CSGOWeaponCase.CASE_ID;
    private static boolean registered;

    /*
     * Overall first-stage rarity targets:
     *
     *  Mil-Spec   : 79.92 %
     *  Restricted : 15.98 %
     *  Classified :  3.20 %
     *  Covert     :  0.64 %
     *  Legendary  :  0.26 %
     *
     * legendary(0.26) is an absolute server-side percentage. The normal
     * entries below deliberately total 99.74, so after the 0.26% legendary
     * check the conditional normal roll still produces the exact overall
     * category percentages above.
     */

    public static final double milSpecWeight = 79.92D / 7.0D;
    public static final double restrictedWeight = 0.98D / 5.0D;
    public static final double classifiedWeight = 3.20D / 3.0D;
    public static final double covertWeight = 15.64D / 2.0D;

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        AgentDossier01.register();
        CSGOWeaponCase.register();
    }
}
