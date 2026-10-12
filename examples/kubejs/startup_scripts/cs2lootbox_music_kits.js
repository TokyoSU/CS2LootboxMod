// Place in kubejs/startup_scripts/. Restart both the client and server after changes.
// Requires CS2 Lootbox v1.1.0+. Definitions and shared geometry stay in the mod;
// model atlases stay in the mod; artwork and audio come from optional client packs.
// Enable individual kit ZIPs or the full collection in Options > Resource Packs.
// Current catalogue: 100 registered kits, 1,548 tracks. Edit existing IDs once.

CS2LootboxEvents.register(event => {
    // Edit an existing kit without replacing its tracks or model.
    event.changeMusicKit('cs2lootbox:music_kit_treeadams_benbromfield_01', kit => {
        kit.imageScale(1.0)
        // kit.image('kubejs:textures/gui/music/custom_cover.png', 256, 198)
        // kit.gui(0, -4, 0, 0, 180, 0, 2)
        // kit.guiCarousel(0, -3.5, 0, 0, 180, 0, 3.5)
        // kit.guiReveal(0, -6, 0, 0, 180, 0, 4.5)
    })

    // Uncomment after adding artwork, mono Ogg Vorbis tracks and sounds.json.
    // See examples/kubejs/README.md. Replace example durations with real seconds.
    // No Java class imports are required.
    /*
    event.addMusicKit('kubejs:my_music_kit', kit => {
        kit.name('My Music Kit')
        kit.rarityGrade('milspec')
        kit.image('kubejs:textures/gui/music/my_cover.png', 256, 198)
        kit.imageScale(1.0)

        // Default: a 2D item using the GUI artwork. Optional 3D item: uncomment
        // BOTH lines after providing an atlas matching this mesh's UV layout.
        // kit.model('cs2lootbox:geo/music_kit_standard_1024x1024.geo.json')
        // kit.texture('kubejs:textures/musickits/my_atlas.png')
        // This layout: cover top left, label top right, record-color swatch below.
        // Original UV canvas is 1024x1024; bundled PNGs are now 512x512.
        // Keep UVs and the record swatch when resizing; don't use image() here.
        // Other meshes (see the layout table in examples/kubejs/README.md):
        //   music_kit_standard_1024x512.geo.json
        //   music_kit_cs2_2048x1024.geo.json
        //   music_kit_valve_csgo_2048x1024.geo.json
        // Rotate this sleeve 180 degrees around Y to show its front cover.
        kit.gui(0, -4, 0, 0, 180, 0, 2)
        // These apply when another case awards the kit. The listening screen
        // uses image(), not the model.
        kit.guiCarousel(0, -4, 0, 0, 180, 0, 2)
        kit.guiReveal(0, -4, 0, 0, 180, 0, 2.5)
        kit.firstPersonRight(0, -2, 0, 0, 180, 0, 2)
        kit.firstPersonLeft(0, -2, 0, 0, 180, 0, 2)
        kit.thirdPersonRight(0, 0, 0, 0, 180, 0, 2)
        kit.thirdPersonLeft(0, 0, 0, 0, 180, 0, 2)
        kit.ground(0, 0, 0, 0, 180, 0, 2)
        kit.fixed(0, -4, 0, 0, 180, 0, 2)

        // Stable ID, displayed title, sound event ID, actual duration in seconds.
        kit.track('mainmenu', 'Main Menu', 'kubejs:music.my_kit.mainmenu', 180.5)
        kit.track('round_win', 'Round Win', 'kubejs:music.my_kit.round_win', 12.25)
    })
    */
    // Track IDs must stay stable: saved choices refer to IDs, not list positions.
    // Editing helpers: clearTracks() and removeTrack(id).
    // Built-ins already exist: use changeMusicKit(), not addMusicKit() again.
    // Optional reward entry after registering the custom kit above:
    // event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
    //     crate.loot('kubejs:my_music_kit', 5)
    // })
})
