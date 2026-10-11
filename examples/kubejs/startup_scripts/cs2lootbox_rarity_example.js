// Optional example: REPLACES the built-in Weapon Case's normal loot table.
// Its existing model, key and legendary table are retained.
// Place in kubejs/startup_scripts/; restart both client and server after changes.

CS2LootboxEvents.register(event => {
    const Grade = Java.loadClass('net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade')
    event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
        crate.clearLoot()
        crate.loot('minecraft:iron_ingot', 40, loot => {
            loot.rarityGrade(Grade.CONSUMER_GRADE)
        });
        crate.loot('minecraft:gold_ingot', 28, loot => {
            loot.rarityGrade(Grade.INDUSTRIAL_GRADE)
        });
        crate.loot('minecraft:diamond', 18, loot => {
            loot.rarityGrade(Grade.MIL_SPEC)
        });
        crate.loot('minecraft:netherite_ingot', 10, loot => {
            loot.rarityGrade(Grade.RESTRICTED)
            // Optional: the configured name is applied to the awarded stack too.
            // loot.name('item.kubejs.some_custom_name');
        });
    });
    // Other shared grades: CLASSIFIED, COVERT, SPECIAL, KNIVES, CONTRABAND.
    // Loot grades set the UI label, color and sound tier; vanilla item rarity
    // remains unchanged. Model items / music kits use shared Minecraft rarities.
});
