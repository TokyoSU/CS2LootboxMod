// Place this file in: kubejs/startup_scripts/cs2lootbox_crates.js
// Startup script changes require a full game restart.

CS2LootboxEvents.register(event => {
    event.addCrate('kubejs:revolution', crate => {
        // Registered items. If omitted, defaults are:
        //   kubejs:revolution_case
        //   kubejs:revolution_key
        crate.caseItem('kubejs:revolution_case')
        crate.keyItem('kubejs:revolution_key')

        // GeckoLib resources. These can live in kubejs/assets/kubejs/.
        crate.model('kubejs:geo/revolution.geo.json')
        crate.texture('kubejs:textures/lootbox/revolution.png')
        crate.animation('kubejs:animations/revolution.animation.json')

        // Normal generated-item texture syntax (no textures/ prefix and no .png).
        crate.keyTexture('kubejs:item/revolution_key')

        // Transform of the 3D model inside the LDLib preview widget.
        crate.position(22, 30)
        crate.scale(78)
        crate.rotation(0, 180, 0) // pitch, yaw, roll

        // GeckoLib animation names inside the animation JSON.
        crate.animations('fall', 'idle', 'open', 'open_idle')
        crate.itemIdleAnimation('idle')

        // CS2Lootbox now ships and registers its CS2-style sounds itself.
        // Nothing is required here for the normal case flow.
        //
        // Optional per-case overrides are still available:
        // crate.openSound('cs2lootbox:case_unlock', 1.0, 1.0)
        // crate.carouselTickSound('cs2lootbox:csgo_ui_crate_item_scroll')
        // These are the AWARDED sounds played when the result UI closes.
        // The roulette stop itself uses the bundled case_reveal_* sounds.
        // crate.rewardSound('cs2lootbox:case_awarded_0_common')
        // crate.uncommonSound('cs2lootbox:case_awarded_1_uncommon')
        // crate.rareSound('cs2lootbox:case_awarded_2_rare')
        // crate.mythicalSound('cs2lootbox:case_awarded_3_mythical')
        // crate.classifiedSound('cs2lootbox:case_awarded_4_legendary')
        // crate.covertSound('cs2lootbox:case_awarded_5_ancient')
        // crate.specialSound('cs2lootbox:csgo_ui_crate_result')

        // Translation keys, not literal English names.
        // Optional here because these are already the default keys for the two item ids above.
        crate.caseName('item.kubejs.revolution_case')
        crate.keyName('item.kubejs.revolution_key')

        // ---------------------------------------------------------------------
        // NORMAL LOOT
        // ---------------------------------------------------------------------
        // Loot entry helpers used below:
        //   loot.count(min, max)
        //   loot.rarity(translationKey, color)
        //   loot.description(translationKey)
        //   loot.nbt('{...}') / loot.tag('{...}')
        //   loot.position(x, y[, z])   // item_list only
        //   loot.rotation(x, y, z)     // item_list only
        //   loot.scale(scale)          // item_list only
        //
        // `crate.loot('#namespace:item_tag', weight, ...)` is also supported.
        // Normal loot weights are normalized only against the NORMAL loot table.
        // The legendary chance below is rolled first and separately, so these
        // relative weights are used only when the legendary roll misses.
        crate.loot('minecraft:iron_ingot', 55, loot => {
            loot.count(1, 4)
            loot.rarity('rarity.kubejs.industrial', 0x5E98D9)
            loot.description('loot.kubejs.iron.description')
        })

        crate.loot('minecraft:gold_ingot', 25, loot => {
            loot.count(1, 3)
            loot.rarity('rarity.kubejs.milspec', 0x4B69FF)
        })

        crate.loot('minecraft:diamond', 12, loot => {
            loot.count(1, 2)
            loot.rarity('rarity.kubejs.classified', 0xD32CE6)
            loot.description('loot.kubejs.diamond.description')

            // Optional transform used ONLY by this entry in the case item_list.
            // Position is an offset in GUI pixels. Rotation is X/Y/Z degrees.
            // Scale 1.0 keeps the default item-list size.
            loot.position(2, -3)
            loot.rotation(0, 25, -8)
            loot.scale(1.15)

            // Optional ItemStack NBT/SNBT. Applied to the item_list preview,
            // carousel, inspector and the final server-authoritative reward.
            loot.nbt('{CustomModelData:123}')
            // `loot.tag('{...}')` is an alias for `loot.nbt('{...}')`.
        })

        crate.loot('minecraft:emerald', 8, loot => {
            loot.count(1)
            loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
            loot.description('loot.kubejs.emerald.description')
        })

        // ---------------------------------------------------------------------
        // ITEM TAG SOURCE
        // ---------------------------------------------------------------------
        // Prefix the item id with # to use a Minecraft/Forge item tag as ONE
        // loot-table entry. If this entry wins, the server randomly chooses
        // one item from that tag.
        //
        // Static UI previews (item_list / tooltip / inspector) use the first
        // registry-sorted member so the preview remains deterministic.
        //
        // Example:
        // crate.loot('#forge:ingots/iron', 10, loot => {
        //     loot.count(1, 3)
        //     loot.rarity('rarity.kubejs.milspec', 0x4B69FF)
        //
        //     // NBT works with tag-backed entries too.
        //     loot.nbt('{CustomModelData:7}')
        // })
        //
        // Tag-backed entries can also be removed later with:
        // crate.removeLoot('#forge:ingots/iron')

        // ---------------------------------------------------------------------
        // LEGENDARY / RARE SPECIAL ITEM
        // ---------------------------------------------------------------------
        // legendary(weight) is an ABSOLUTE first-carousel percentage.
        // It is NOT added to the normal loot total. Here 0.26 means exactly 0.26%.
        //
        // subLoot() is a separate second-stage WEIGHT table. It does not need to
        // total 100; the entries are normalized only against each other. With
        // weights 5 and 4 below, the final legendary reward odds are 55.56% / 44.44%.
        //
        // Sound flow:
        //   first carousel hits Legendary -> case_awarded_4_legendary_01
        //   final carousel stops on item   -> case_reveal_* for that rarity
        //   result UI closes               -> case_awarded_* for that rarity
        //
        // Legendary sub-loot defaults to Covert rarity if no rarity is supplied,
        // which means its reveal/awarded sound uses the Ancient sound set.
        // The primary carousel uses the absolute chance as a visual density and
        // spaces rare gold panels with an accumulator instead of independently
        // rolling every filler slot. This avoids unrealistic clusters. At 0.26%,
        // a filler gold panel averages roughly once every 385 carousel slots.
        // The server-selected winning slot is always forced.
        //
        // tooltip() accepts literal text or a translation key. The same text
        // is used in the case tooltip AND on the gold primary-carousel card.
        //
        // foreground() is optional. When omitted, the mod automatically uses:
        //   cs2lootbox:textures/gui/default_rare_item.png
        //
        // The second legendary-only carousel is enabled by default. Use
        // subLootCarousel(false) to skip it and reveal the server-selected
        // sub-loot item directly after the primary gold card stops.
        crate.legendary(0.26)
            .itemListName('tooltip.kubejs.rare_item')
            .tooltip('tooltip.kubejs.rare_item')
            .subLootCarousel(false)
            .subLoot()
                .add('minecraft:netherite_ingot', 5.0, loot => {
                    loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
                    loot.description('loot.kubejs.netherite.description')
                })
                .add('minecraft:nether_star', 4.0, loot => {
                    loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
                    loot.description('loot.kubejs.nether_star.description')

                    // NBT/tag data is supported by legendary sub-loot as well.
                    loot.tag('{CustomModelData:999}')
                })

        // A second legendary() declaration is also supported. Legendary panels
        // are ALWAYS appended after every normal item, in declaration order:
        //   [normal items...] [legendary #1] [legendary #2] ...
        //
        // To use a custom icon for another legendary panel:
        // crate.legendary(0.10)
        //     .foreground('kubejs:textures/gui/my_rare_item.png')
        //     .subLoot()
        //         .add('minecraft:dragon_egg', 50.0)
        //         .add('minecraft:elytra', 50.0)
        //
        // Multiple legendary() panels use separate absolute chance bands. Their
        // combined legendary chance must remain <= 100%. Normal loot weights are
        // still independent and do not need rebalancing.

        // Optional tuning:
        // crate.light(12, 12)
        // crate.caseStackSize(16)
        // crate.keyStackSize(64)
    })
})
