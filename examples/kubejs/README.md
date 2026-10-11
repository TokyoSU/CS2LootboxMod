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

The catalogue and shared geometry stay in the mod. Audio and artwork are optional
client resources. Enable individual kit packs or `cs2lootbox-music-full.zip` for
all 101 kits through **Options > Resource Packs**. Dedicated servers do not need
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

See the [main README](../../README.md) and [music-kit guide](../../docs/music-kits.md)
for the full setup and packaging commands.
