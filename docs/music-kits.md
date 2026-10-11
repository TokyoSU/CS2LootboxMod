# Music kits

Requires **CS2 Lootbox v1.1.0+** and its dependencies on Minecraft Forge 1.20.1.

Right-click a music kit to open its listening screen. It uses translucent gray panels, pale blue selection and progress highlights, an orange scrollbar and a green Save button. Kits have no opening carousel.

Select a track in the list to play it. Previous/Next change tracks; Play changes to Stop during playback; Pause changes to Resume. The progress clock starts when the audio actually starts and stops advancing while paused. Closing the screen stops its preview.

Save keeps the selected track on the held kit, then closes the screen. Only the track ID is stored: inserting the kit in a vanilla jukebox always starts that track at zero. Preview playback is local; saving and jukebox insertion are checked by the server.

## Optional music resource packs

The mod stores the music-kit definitions, item IDs and shared sleeve/record geometry. Audio, cover artwork and texture atlases live in optional resource packs under `resourcepacks/cs2lootbox-music/`; they are not bundled in the mod JAR. Installing several kit packs does not duplicate the shared geometry.

Put the mod JAR in `mods/` and a music pack ZIP in `resourcepacks/`, then enable it in Minecraft's **Options → Resource Packs**. Only kits with their required artwork, model textures and all track resources appear in creative tabs. Without a music pack, the built-in music kits are hidden and their listening screens are unavailable. Cases and other mod items still work normally.

For the CurseForge app, install **the full collection** (`cs2lootbox-music-full.zip`) to enable all 101 kits together. Individual kit ZIPs remain available for manual installation. The full pack contains the same assets and quality, with all sound events merged into one manifest; shared geometry is still supplied by the mod.

Changing enabled packs or pressing **F3+T** refreshes kit availability. Item IDs remain registered for saved worlds and multiplayer compatibility: an existing kit whose resources are removed gets a vanilla disc icon and cannot open its listening screen. Re-enable its pack to restore it. A client without a kit's resources cannot hear that kit in a jukebox.

Dedicated servers do not need the music pack. Players install the packs they want locally, or a server can distribute them through Minecraft's normal server resource-pack setting. Clients and servers still need matching mod/KubeJS definitions.

Build all kits as separate optional packs:

```bat
gradlew.bat musicResourcePacks
```

`musicResourcePack` is an alias for the same task. The output is **one ZIP per kit** in `build/resourcepacks/`, named `cs2lootbox-music-<kit>.zip`. Each pack has that kit's GUI artwork as its `pack.png` icon and its music-kit title as the description. Enable any combination of packs to choose the kits available in-game.

The Python builder also produces a separate ZIP for each requested kit and needs no additional libraries:

```bat
python tools/build_music_resource_pack.py
python tools/build_music_resource_pack.py --kit treeadams_benbromfield_01
python tools/build_music_resource_pack.py --kit denzelcurry_01 --kit bbnos_01
```

Omit `--kit` to build all individual packs, or repeat it to select which packs to build. Full item IDs also work. `--output-dir` chooses the destination directory; `--output` names a single ZIP when exactly one kit is selected. Each pack contains only its own audio, artwork, optional atlas texture and sound definitions, plus its icon. Shared geometry is stored once in the mod. All packs use Minecraft 1.20.1's resource-pack format 15. Audio and texture quality are preserved.

Build the full collection as one resource pack:

```bat
gradlew.bat musicResourcePackFull
python tools/build_music_resource_pack.py --combined
```

Use either command. The output is `build/resourcepacks/cs2lootbox-music-full.zip`.
The Python builder also supports `--combined --kit <id> --kit <id>` for a selected
collection and `--output <file.zip>` to name the combined ZIP.

## Available kits

`cs2lootbox:music_kit_treeadams_benbromfield_01` appears in the CS2 Lootbox creative tab when its resources are enabled. It uses the shared converted sleeve/record mesh from the mod, its original 1024×1024 atlas, the 256×198 artwork and 15 streaming tracks from its pack.

The library also includes all 100 other kits available in the extracted game folder, with 1,548 additional tracks. Their IDs use `cs2lootbox:music_kit_<source folder name>`. With all kit packs enabled, all kits appear in the creative tab, open the listening screen and support saving a song for jukebox playback. All supplied tracks are mono Ogg Vorbis with streaming enabled.

26 additional kits reuse the existing mesh with a matching texture atlas, preserving source texture pixels. The other 74 use their own inventory artwork as a 2D item until their models are converted. See the [missing-model list](music-kit-missing-models.md) for exact filenames and compatibility reasons. Source images are not resized to force them onto an incompatible model.

The original Tree Adams / Ben Bromfield kit is registered once in Java; `data/cs2lootbox/music_kits.json` supplies the other 100 kits and excludes that original ID. All are registered before KubeJS startup callbacks, so `changeMusicKit` works on every kit. `tools/import_music_kits.py` rebuilds the catalogue from extracted files using Pillow and VLC; it leaves the source game folder untouched and resumes completed mono conversions.

For a kit on the missing-model list, export its matching model and atlas together, then assign them with `changeMusicKit`. Shared geometry can stay in the mod while each kit pack supplies its own atlas. Set its display transforms as in the 3D example below; a kit that previously used 2D artwork has no saved 3D transforms. Keep the existing track list to preserve saved jukebox selections.

The visible kit keeps its 3D appearance in inventory, in hand and when dropped. Inside a jukebox it uses a hidden vanilla-style disc for the chosen track. Ejecting it or breaking the jukebox returns the kit, preserving its custom name and NBT. Hidden discs are not added to the mod's creative tab.

## KubeJS registration

Place scripts in `kubejs/startup_scripts/`. Restart the game and dedicated server after changing kit definitions. Clients and servers need matching startup definitions.

```js
CS2LootboxEvents.register(event => {
    event.addMusicKit('kubejs:my_music_kit', kit => {
        kit.name('My Music Kit')
        kit.rarityGrade('milspec')
        kit.image('kubejs:textures/gui/music/my_cover.png', 256, 198)
        kit.imageScale(1.0)
        kit.model('cs2lootbox:geo/music_kit_treeadams_benbromfield_01.geo.json')
        kit.texture('kubejs:textures/musickits/my_atlas.png')
        kit.gui(0, -4, 0, 0, 180, 0, 2)
        kit.firstPersonRight(0, -2, 0, 0, 180, 0, 2)
        kit.firstPersonLeft(0, -2, 0, 0, 180, 0, 2)
        kit.track('mainmenu', 'Main Menu', 'kubejs:music.my_kit.mainmenu', 180.5)
        kit.track('round_win', 'Round Win', 'kubejs:music.my_kit.round_win', 12.25)
    })
})
```

No Java class imports are needed in these scripts. The first argument to `addMusicKit` is also the visible item ID. Track IDs must be unique within the kit and use lowercase letters, digits, `_`, `-` or `/`. Keep those IDs stable when reordering songs: saved choices refer to IDs, rather than list positions. Kits require at least one track.

The last argument to `track` is the audio's duration in **seconds**, used by the progress bar and vanilla jukebox timer. Use the actual duration, including silence. `name` and track titles accept literal text or translation keys; strings containing a dot are treated as translation keys.

`model` and `texture` are optional, but must be supplied together. Without them, the kit uses its artwork as a 2D item texture. `itemTexture('kubejs:item/my_kit')` can override that fallback separately. Hidden jukebox discs always use Minecraft's disc appearance.

`image(path, width, height)` uses a full `textures/...png` resource path and the actual PNG dimensions. Images retain their aspect ratio. `imageScale` accepts values above zero up to 2; values above 1 enlarge and crop the image within its panel. `image(path)` defaults to 512×512.

Model transforms use the same arguments as other model items: `x, y, z, rotationX, rotationY, rotationZ, scale`. Supported methods are `gui`, `guiCarousel`, `guiReveal`, `firstPersonRight`, `firstPersonLeft`, `thirdPersonRight`, `thirdPersonLeft`, `ground` and `fixed`. Carousel/reveal transforms apply when the kit is another case's reward; the listening screen uses its PNG artwork.

The included sleeve mesh uses a 180-degree Y rotation to show its front cover. Apply that rotation when reusing this mesh for another kit.

Built-in kits are registered before the startup event, so they can be edited:

```js
CS2LootboxEvents.register(event => {
    event.changeMusicKit('cs2lootbox:music_kit_treeadams_benbromfield_01', kit => {
        kit.image('kubejs:textures/gui/music/my_cover.png', 256, 198)
        kit.imageScale(0.9)
        // kit.clearTracks()
        // kit.track('my_song', 'My Song', 'kubejs:music.my_song', 123.4)
        // kit.removeTrack('deathcam')
    })
})
```

Existing settings are copied before changes. Add kits to a case's loot with their visible item ID, for example `crate.loot('kubejs:my_music_kit', 10)`.

## Audio assets

Minecraft Java reads **Ogg Vorbis (`.ogg`)**. Convert WAV, MP3 or other formats to Vorbis; renaming an extension is insufficient. Use `stream: true` for music so Minecraft streams the track instead of decoding the entire song into memory. Ogg Opus is not the same codec.

Use **mono** audio for jukebox tracks: stereo streams do not receive normal distance attenuation. The included music kit uses mono Vorbis at 44.1 kHz; the listening GUI plays these same tracks without distance attenuation.

Example files:

```text
kubejs/
  startup_scripts/cs2lootbox_music_kits.js
  assets/kubejs/
    sounds.json
    sounds/music/my_kit/mainmenu.ogg
    sounds/music/my_kit/round_win.ogg
    textures/gui/music/my_cover.png
    textures/musickits/my_atlas.png
```

`kubejs/assets/kubejs/sounds.json`:

```json
{
  "music.my_kit.mainmenu": {
    "sounds": [{ "name": "kubejs:music/my_kit/mainmenu", "stream": true }]
  },
  "music.my_kit.round_win": {
    "sounds": [{ "name": "kubejs:music/my_kit/round_win", "stream": true }]
  }
}
```

The script uses the **sound event ID** (`kubejs:music.my_kit.mainmenu`), while `sounds.json` refers to the **audio path** (`kubejs:music/my_kit/mainmenu`, without `sounds/` or `.ogg`). Only clients need to decode the audio, but all players need the same audio resources to hear jukebox playback.

Preview and jukebox volume use Minecraft's **Jukebox/Note Blocks** slider. A missing sound or disabled audio is shown as an error in the preview.

## Validation

`gradlew.bat musicKitAudioCheck runGameTestServer` checks the playback clock, decodes every included track with Minecraft's Vorbis reader, verifies mono format and catalogue durations, and runs dedicated-server tests for the full catalogue, KubeJS callbacks, the screen's Save validation, saved NBT, track reordering, hidden-disc tags, jukebox timing and ejection. The six dedicated-server tests also check availability with no pack, the complete source asset library, missing audio/artwork/geometry, and independently built single-kit packs, including enabling several packs together. The test task automatically builds the two individual packs used by these checks. The GameTest server uses an isolated world under `build/music-kit-gametest`.
