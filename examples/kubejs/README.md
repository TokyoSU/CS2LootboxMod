# KubeJS examples for CS2 Lootbox v1.1.0+

Copy only the examples you want into your instance's `kubejs/startup_scripts/`.
Merge translations from `assets/kubejs/lang/en_us.json` with your existing keys.
Restart both client and dedicated server after script changes; `/reload` does
not register items. Keep mod versions and startup definitions matching.

| Script | Effect | Assets needed |
| --- | --- | --- |
| `cs2lootbox_crates.js` | Adds a Revolution case and key | Your mesh, atlas, animation JSON and key PNG at the paths in the script |
| `cs2lootbox_rarity_example.js` | Replaces the built-in Weapon Case's normal loot table | None; uses shared grades and built-in case assets |
| `cs2lootbox_items.js` | Adds a static patch and adds it to the Patch Pack loot list | Included mod assets; example language file |
| `cs2lootbox_music_kits.js` | Edits the existing Tree Adams / Ben Bromfield kit; includes a commented custom-kit registration | That kit's resource pack; custom assets before enabling the custom example |

`CS2LootboxEvents.register` exposes `addCrate` / `addCase`, `changeCase`,
`addItem`, `addMusicKit` and `changeMusicKit`. Built-ins are registered before
the event. Edit them instead of registering their IDs twice. Callback arguments
do not need Java class names or imports. Shared-grade examples use `Java.loadClass`
because loot entries accept a `LootRarityGrade` enum; music kits accept string
grade IDs such as `'milspec'`.

## Music assets

The catalogue, shared geometry and built-in model atlases stay in the mod. Audio and listening-screen artwork are optional
client resources. Enable individual kit packs or `cs2lootbox-music-full.zip` for
all 100 registered kits and their 1,548 tracks through **Options > Resource Packs**. Dedicated servers do not need
these ZIPs. Missing resources hide a kit and disable listening; existing IDs and
saved selections are preserved. Change packs or use **F3+T** to refresh availability.

For the commented custom kit, provide:

```text
kubejs/assets/kubejs/
    sounds.json
    sounds/music/my_kit/mainmenu.ogg
    sounds/music/my_kit/round_win.ogg
    textures/gui/music/my_cover.png
    textures/musickits/my_atlas.png    (only when enabling the 3D model)
```

Merge the example `sounds.json` with your existing sound definitions. Supply your
own PNG and audio files; they are not included in these examples. Audio must be
**Ogg Vorbis**, with mono channels for jukebox distance attenuation and
`stream: true` for streaming. Use the PNG's actual dimensions in `image(...)` and
the audio's actual duration in seconds in `track(...)`, including silence.
Keep track IDs stable when reordering songs.

`image(...)` controls the listening screen; `texture(...)` controls the 3D model.
Register both `model(...)` and `texture(...)` to use a 3D kit. Its shared model
does not need to be copied into each resource pack. Built-in kit texture names
match their IDs: `cs2lootbox:textures/musickits/music_kit_<kit_id>.png`.

The mod has four shared music meshes. Choose the UV layout rather than the PNG
size alone: several layouts have the same aspect ratio. Filenames describe the
original UV reference canvas; bundled PNGs now have half those dimensions.

| Model under `cs2lootbox:geo/` | Current bundled PNG | Layout |
| --- | --- | --- |
| `music_kit_standard_1024x1024.geo.json` | 512×512 | Cover top left, label top right |
| `music_kit_standard_1024x512.geo.json` | 512×256 | Label on the left, small cover on the right |
| `music_kit_cs2_2048x1024.geo.json` | 1024×512 | Large cover on the left, label at the upper right |
| `music_kit_valve_csgo_2048x1024.geo.json` | 1024×512 | Valve 01/02 display shape and large-cover layout |

Keep geometry UV reference dimensions and UV coordinates unchanged when resizing
an atlas proportionally. Preserve the tiny record-color swatch; see the
[layout and legacy-path guide](../../docs/music-kits.md#shared-model-layouts).
Older Tree Adams, Lennie Moore, Adam Beyer and Valve 01 model paths still resolve
to these shared meshes through the music-kit builder.

To distribute these assets as an optional resource pack, put `assets/kubejs/`
at the ZIP root alongside `pack.mcmeta`, using `pack_format: 15`. Keep registration
in startup scripts on both sides. GUI artwork uses a full
`namespace:textures/...png` path; sound events use `namespace:event.name`;
`sounds.json` audio paths omit `sounds/` and `.ogg`.

## Display options

`gui`, `guiCarousel` and `guiReveal` separately control inventory, roulette and
result-panel model rendering. Arguments are position X/Y/Z, rotation X/Y/Z in
degrees and scale. Carousel and reveal fall back to `gui` when omitted.
`collectionImageScale` controls the case's collection PNG; music-kit `imageScale`
controls listening-screen artwork while preserving its aspect ratio. Music
image values above 1 crop within the panel; the maximum is 2.

`noDropSound()` silences a case's falling sound. Global rarity grades are reused
by the patch/model-item and loot examples; choose a shared grade instead of
repeating its label and RGB value for each entry.

The current built-in atlases and key icons are already optimized. Texture
resolution does not change model polygon counts or animation data. In Creative
mode, `/cs2lootbox renderstats` reports GPU-cache, pose and reusable-buffer
statistics; see the [rendering guide](../../docs/held-case-rendering.md).

See the [main README](../../README.md) and [music-kit guide](../../docs/music-kits.md)
for the full setup and packaging commands.
