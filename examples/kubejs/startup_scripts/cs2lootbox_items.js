// Static patch reward using assets included in the mod.
// Place in kubejs/startup_scripts/; restart both client and server after changes.

CS2LootboxEvents.register(event => {
    const Grade = Java.loadClass('net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade')
    event.addItem('kubejs:my_patch', item => {
        item.name('item.kubejs.my_patch')
        item.stackSize(1)
        item.rarityGrade(Grade.MIL_SPEC)
        item.model('cs2lootbox:geo/patch_inspect.geo.json')
        item.texture('cs2lootbox:textures/patches/patch_shatteredweb.png')

        // x, y, z, rotationX, rotationY, rotationZ, scale
        item.gui(0, -9.5, 0, 0, -90, 0, 10)
        item.guiCarousel(0, -15.5, 0, 0, -90, 0, 18)
        item.guiReveal(0, -25.5, 0, 0, -90, 0, 25)
        item.fixed(0, -9.5, 0, 0, -90, 0, 10)
        item.ground(0, 0, 0, 0, -90, 0, 4)
        item.firstPersonRight(0, -1.2, 0, 0, -90, 0, 4)
        item.firstPersonLeft(0, -1.2, 0, 0, -90, 0, 4)
        item.thirdPersonRight(0, 0, 0, 0, -90, 0, 4)
        item.thirdPersonLeft(0, 0, 0, 0, -90, 0, 4)
    })

    event.changeCase('cs2lootbox:csgo_patch_pack', crate => {
        crate.loot('kubejs:my_patch', 10, loot => {
            loot.rarityGrade(Grade.MIL_SPEC)
            loot.position(0, 8)
            loot.scale(2)
        })
    })
})
