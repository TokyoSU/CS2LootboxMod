// CS2 grade example. Built-in crate sounds are supplied by the mod by default.

LootboxStartupEvents.register(event => {
    event.create('kubejs:revolution', crate => {
        crate.loot('minecraft:iron_ingot', 40, loot => {
            loot.grade('consumer');
        });
        crate.loot('minecraft:gold_ingot', 28, loot => {
            loot.grade('industrial');
        });
        crate.loot('minecraft:diamond', 18, loot => {
            loot.grade('milspec');
        });
        crate.loot('minecraft:netherite_ingot', 10, loot => {
            loot.grade('restricted');
            // Optional: the configured name is applied to the awarded stack too.
            // loot.name('item.kubejs.some_custom_name');
        });
    });
});
