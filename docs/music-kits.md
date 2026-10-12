# Music kits

Listen to a kit's tracks, save your favorite song and play it in a vanilla jukebox.

| At a glance | |
| --- | --- |
| Requires | **CS2 Lootbox v1.1.0+** and its [dependencies](../README.md#requirements) |
| Platform | Minecraft **1.20.1**, Forge **47.x** |
| Collection | **100 registered kits · 1,548 tracks** |
| Playback | Listening GUI and vanilla jukeboxes |
| Customization | KubeJS artwork, tracks, rarity grades and 3D models |

## Contents

- [Install and play](#install-and-play)
- [Listening controls](#listening-controls)
- [Optional music resource packs](#optional-music-resource-packs)
- [KubeJS registration](#kubejs-registration)
- [Audio assets](#audio-assets)
- [Shared-model layouts](#shared-model-layouts)
- [Build resource packs](#build-resource-packs)
- [Troubleshooting](#troubleshooting)
- [Validation](#validation)

## Install and play

1. Install CS2 Lootbox and its dependencies in your instance's `mods/` folder.
2. Put an individual music-kit ZIP or **`cs2lootbox-music-full.zip`** in `resourcepacks/`.
3. Enable the pack in **Options → Resource Packs**.
4. Find an available kit in the **CS2 Lootbox creative tab**, or obtain it through your modpack's configured rewards.
5. **Right-click** the kit, choose a track and select **Save**.
6. Insert the kit into a **vanilla jukebox** to play the saved track from the beginning.

> **Using the CurseForge app?** Choose the **Full Collection** pack to enable the registered collection together. Individual ZIPs are also available for manual installation.

Ejecting the disc or breaking the jukebox returns the kit with its saved selection, custom name and NBT. Saving stores the track ID rather than the preview's playback position.

## Listening controls

The CS2-style screen uses translucent gray panels, pale blue selection and progress highlights, an orange scrollbar and a green Save button. Music kits open this screen directly, without an opening carousel.

| Control | What it does |
| --- | --- |
| Track list | Select a track and start its local preview |
| Previous / Next | Change the selected track |
| Play / Stop | Start playback or stop the current preview |
| Pause / Resume | Pause playback or continue from the paused position |
| Progress bar | Show playback time; the clock starts with the audio and pauses with it |
| Save | Keep the selected track on the held kit and close the screen |
| Close | Stop the local preview and close the screen |

Preview and jukebox volume follow Minecraft's **Jukebox/Note Blocks** slider. Preview playback is local; saving and jukebox insertion are validated by the server. A kit can also be another case's reward, using that case's opening flow.

## Optional music resource packs

### Choose your collection

| Pack | Contents | Best for |
| --- | --- | --- |
| `cs2lootbox-music-<kit_id>.zip` | One kit's audio, listening artwork, matching atlas and sound definitions | Selecting individual kits |
| `cs2lootbox-music-full.zip` | The source music library with a merged sound manifest | Installing the registered collection together |

Individual packs use their kit's listening artwork as the pack icon. You can enable several individual packs together.

### What belongs where?

| In the mod | In the optional music packs |
| --- | --- |
| Kit definitions and item IDs | Streaming audio and sound definitions |
| Shared sleeve-and-record geometry | Listening-screen artwork |
| Built-in model atlases | Matching atlas copies |
| Saving and jukebox support | Each player's enabled music collection |

Shared geometry is stored once in the mod. **Update music packs alongside the mod** so their atlas copies use the optimized textures too.

### Availability and multiplayer

- Kits become available when their required artwork, model textures and all track resources are present.
- Changing enabled packs or pressing **F3+T** refreshes availability.
- Disabling a pack preserves existing item IDs and saved selections. An unavailable kit uses a vanilla disc icon and cannot open its listening screen; re-enable its resources to restore it.
- A client needs a kit's audio resources to hear that kit's jukebox playback.
- Dedicated servers do not need music packs. Players can install them locally, or a server can distribute a pack through Minecraft's server resource-pack setting.
- Clients and servers need matching mod versions and KubeJS startup definitions.

The [catalogue](../src/main/resources/data/cs2lootbox/music_kits.json) registers **100 kits and 1,548 tracks**, including `cs2lootbox:music_kit_treeadams_benbromfield_01`. The source audio folders and **101 atlas PNGs** are asset inventories, rather than extra registered kits.

## KubeJS registration

Place scripts in `kubejs/startup_scripts/`. Restart the client and server after definition changes; `/reload` does not register new items. Callback arguments need no Java class imports.

### 1. Prepare the assets

```text
kubejs/
├── startup_scripts/
│   └── cs2lootbox_music_kits.js
└── assets/kubejs/
    ├── sounds.json
    ├── sounds/music/my_kit/
    │   ├── mainmenu.ogg
    │   └── round_win.ogg
    ├── textures/gui/music/
    │   └── my_cover.png
    └── textures/musickits/
        └── my_atlas.png              # Optional: only for a 3D kit
```

Use your PNG's actual dimensions and each track's actual duration. The following durations are examples to replace with your own.

### 2. Register a custom kit

This example uses its listening artwork as a 2D item texture. Add the optional model settings below to give it a 3D appearance.

```js
CS2LootboxEvents.register(event => {
    event.addMusicKit('kubejs:my_music_kit', kit => {
        kit.name('My Music Kit')
        kit.rarityGrade('milspec')
        kit.image('kubejs:textures/gui/music/my_cover.png', 256, 198)
        kit.imageScale(1.0)
        kit.track('mainmenu', 'Main Menu', 'kubejs:music.my_kit.mainmenu', 180.5)
        kit.track('round_win', 'Round Win', 'kubejs:music.my_kit.round_win', 12.25)
    })
})
```

The first argument is the visible item ID. A kit needs at least one track. Track IDs must be unique and use lowercase letters, digits, `_`, `-` or `/`.

> **Keep track IDs stable.** Saved jukebox selections refer to IDs, not list positions. Reordering tracks is safe when their IDs stay the same.

### 3. Add a 3D model (optional)

Add these settings **inside the `addMusicKit` callback**, after supplying an atlas compatible with the selected [shared layout](#shared-model-layouts):

```js
kit.model('cs2lootbox:geo/music_kit_standard_1024x1024.geo.json')
kit.texture('kubejs:textures/musickits/my_atlas.png')

// x, y, z, rotationX, rotationY, rotationZ, scale
kit.gui(0, -4, 0, 0, 180, 0, 2)
kit.guiCarousel(0, -4, 0, 0, 180, 0, 2)
kit.guiReveal(0, -4, 0, 0, 180, 0, 2.5)
kit.firstPersonRight(0, -2, 0, 0, 180, 0, 2)
kit.firstPersonLeft(0, -2, 0, 0, 180, 0, 2)
```

Supply **both** `model(...)` and `texture(...)`. The shared sleeve meshes use a **180° Y rotation** to show their front cover. The listening screen continues to display `image(...)`, independently of the 3D model.

| Setting | Purpose |
| --- | --- |
| `image(path, width, height)` | Listening artwork; full `textures/...png` path and actual PNG dimensions |
| `imageScale(value)` | Artwork scale, greater than 0 and at most 2; values above 1 enlarge and crop within the panel |
| `itemTexture(path)` | Override the 2D fallback with an item-texture ID such as `kubejs:item/my_kit` |
| `model(path)` + `texture(path)` | Optional 3D geometry and its matching atlas |
| `gui(...)` | Inventory model transform |
| `guiCarousel(...)` / `guiReveal(...)` | Model transforms when a case awards the kit |
| `firstPersonRight(...)` / `firstPersonLeft(...)` | Held model transforms in first person |
| `thirdPersonRight(...)` / `thirdPersonLeft(...)` | Held model transforms in third person |
| `ground(...)` / `fixed(...)` | Dropped-item and item-frame model transforms |

`image(path)` defaults to 512×512. Names and track titles accept literal text or translation keys; dotted strings are treated as translation keys. Hidden jukebox discs use a vanilla disc appearance and are excluded from creative tabs.

### Edit an existing kit

Built-ins are registered before startup callbacks. Use `changeMusicKit(...)` to edit them, rather than registering their IDs again. Existing settings are copied before your changes.

```js
CS2LootboxEvents.register(event => {
    event.changeMusicKit('cs2lootbox:music_kit_treeadams_benbromfield_01', kit => {
        kit.imageScale(0.9)
        // Optional: provide this artwork before enabling the override.
        // kit.image('kubejs:textures/gui/music/my_cover.png', 256, 198)
    })
})
```

Use `removeTrack(id)` before replacing an existing track with the same ID. `clearTracks()` removes the entire list; add at least one track afterward. Add a kit to a case's loot using its visible ID, for example `crate.loot('kubejs:my_music_kit', 10)` after registering the custom kit.

See the [ready-to-copy startup script](../examples/kubejs/startup_scripts/cs2lootbox_music_kits.js) and [KubeJS setup guide](../examples/kubejs/README.md) for more examples.

## Audio assets

| Requirement | Use |
| --- | --- |
| Codec | **Ogg Vorbis (`.ogg`)**; convert WAV/MP3 rather than renaming the extension |
| Channels | **Mono** for normal jukebox distance attenuation |
| Loading | **`stream: true`** in `sounds.json` |
| Duration | Actual audio length in **seconds**, including silence, in `track(...)` |

Ogg Opus is a different codec. The listening GUI plays the same mono tracks without distance attenuation; jukeboxes use positional playback.

Merge these entries into `kubejs/assets/kubejs/sounds.json`:

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

| Where | Example | Meaning |
| --- | --- | --- |
| KubeJS `track(...)` | `kubejs:music.my_kit.mainmenu` | Sound **event ID** |
| `sounds.json` | `kubejs:music/my_kit/mainmenu` | Audio path, without `sounds/` or `.ogg` |
| File | `assets/kubejs/sounds/music/my_kit/mainmenu.ogg` | Actual audio resource |

For an optional custom resource pack, place `assets/kubejs/` at the ZIP root alongside `pack.mcmeta`, with **`pack_format: 15`**. Keep registration scripts on both the client and server. Clients need the audio assets to hear playback.

## Shared-model layouts

All 100 registered kits use compatible shared display meshes. These meshes may differ from an original kit's source geometry; no further conversions are needed for the current shared display shapes. See the [model status](music-kit-missing-models.md).

**Model names retain the original UV canvas dimensions.** Bundled PNGs have half the original width and height. Proportional resizing preserves normalized UV mapping, so leave geometry UV coordinates and reference dimensions unchanged.

| Model under `cs2lootbox:geo/` | Kits | Current PNG | Packing |
| --- | ---: | --- | --- |
| `music_kit_standard_1024x1024.geo.json` | 28 | 512×512 | Cover top left; label top right |
| `music_kit_standard_1024x512.geo.json` | 37 | 512×256 | Label left; smaller cover right |
| `music_kit_cs2_2048x1024.geo.json` | 33 | 1024×512 | Large cover left; label upper right |
| `music_kit_valve_csgo_2048x1024.geo.json` | 2 | 1024×512 | Valve 01/02 display shape with large-cover packing |

Choose compatible cover/label positions, rather than matching only texture size or aspect ratio. Each kit uses `cs2lootbox:textures/musickits/music_kit_<kit_id>.png`.

<details>
<summary><strong>Original atlas coordinates and resizing</strong></summary>

Coordinates below refer to the original UV canvas, before resizing.

| Canvas | Cover | Label | Record-color swatch |
| --- | --- | --- | --- |
| 1024×1024 | 512×512 at (0, 0) | 512×512 at (512, 0) | (0, 512) |
| 1024×512 | 256×256 at (512, 0) | 512×512 at (0, 0) | (768, 0) |
| 2048×1024 | 1024×1024 at (0, 0) | 512×512 at (1024, 0) | (1536, 0) |

Preserve the tiny opaque record-color swatch when resizing. [`tools/resize_textures.py`](../tools/resize_textures.py) filters regions separately, protects the swatch and keeps full-resolution backups. See [texture optimization](texture-optimization.md).

</details>

<details>
<summary><strong>Legacy model names and per-kit overrides</strong></summary>

Older music-kit `model(...)` paths still resolve to the renamed shared meshes. All filenames below use the `cs2lootbox:geo/` prefix and `.geo.json` extension.

| Legacy basename | Shared basename |
| --- | --- |
| `music_kit_treeadams_benbromfield_01` | `music_kit_standard_1024x1024` |
| `music_kit_lenniemoore_01` | `music_kit_standard_1024x512` |
| `music_kit_adambeyer_01` | `music_kit_cs2_2048x1024` |
| `music_kit_valve_01` | `music_kit_valve_csgo_2048x1024` |

Use shared names in new scripts. An exact per-kit geometry file under `assets/cs2lootbox/geo/music_kit_<kit_id>.geo.json` takes priority over the catalogue's shared model when supplied in the mod.

`valve_02` uses the Valve 01 3D cover and model as an explicit fallback, while retaining its own listening artwork and tracks. `valve_cs2_01` uses the CS2 group separately. Preserve track IDs and the existing track list when changing a model.

</details>

## Build resource packs

These commands require the source library at `resourcepacks/cs2lootbox-music/`. A normal mod build works without the optional library.

| Build | Command |
| --- | --- |
| Full Collection | `gradlew.bat musicResourcePackFull` |
| All individual packs | `gradlew.bat musicResourcePacks` |
| One kit | `gradlew.bat musicResourcePack_treeadams_benbromfield_01` |
| Selected kits | `gradlew.bat musicResourcePack_denzelcurry_01 musicResourcePack_bbnos_01` |

Outputs go to **`build/resourcepacks/`**. The full pack is named `cs2lootbox-music-full.zip`; individual packs use `cs2lootbox-music-<kit_id>.zip`. `musicResourcePack` is an alias for `musicResourcePacks`.

Tasks are created from source audio folders, so their count can differ from the registered catalogue. On Linux/macOS, replace `gradlew.bat` with `./gradlew`.

## Troubleshooting

| Problem | Check |
| --- | --- |
| Kit missing from the creative tab | Enable its pack and check that artwork, atlas and every track resource are present; refresh with **F3+T** |
| Existing kit shows a vanilla disc icon | Restore its missing resources or re-enable its pack |
| Preview has no sound | Check the **Jukebox/Note Blocks** slider, sound event ID, audio file and Vorbis codec; the GUI reports missing or disabled audio |
| Jukebox audio carries too far | Convert stereo audio to **mono Vorbis** for distance attenuation |
| Custom kit does not register | Use a startup script and fully restart; provide at least one track and unique track IDs |
| Model shows its back | Use the shared mesh's **180° Y rotation** in its display transforms |
| 3D cover looks incorrectly mapped | Match the atlas layout to the mesh; use `texture(...)` for the atlas and `image(...)` for listening artwork |
| Old large textures return after updating | Update enabled music packs alongside the mod; their atlas copies can override the mod's PNGs |

## Validation

After changing a kit:

1. Restart the client and server, then enable the required resources.
2. Check artwork, track selection, playback controls and Save.
3. Insert and eject the kit in a jukebox; check volume and distance attenuation.
4. Reload resources to confirm availability refreshes.
5. If tracks were reordered or replaced, check an existing saved kit still selects the intended track by ID.

For rendering diagnostics in Creative mode, use **`/cs2lootbox renderstats`**. Source checks are available with:

```bat
gradlew.bat renderBufferPoolCheck renderCommandCheck --offline
```

These checks exercise buffer reuse, cached geometry and command access; they do not measure in-game FPS. See the [rendering guide](held-case-rendering.md) and [native-memory audit](native-memory-audit.md).
