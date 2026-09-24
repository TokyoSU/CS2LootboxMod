# KubeJS Integration

CS2 Lootbox includes a **startup-time KubeJS API** for creating complete CS2-style cases without writing Java.

With KubeJS you can:

- register new case items and optional matching key items automatically;
- use custom GeckoLib models, textures and animations;
- configure the 3D case preview;
- build weighted loot tables;
- use Minecraft/Forge item tags as rewards;
- add custom NBT to rewards;
- configure CS2-style rarity names and colors;
- create legendary / rare-special-item stages;
- optionally skip the second legendary sub-loot carousel;
- configure separate translated legendary names for the case contents `item_list` and case-item hover tooltip;
- add StatTrak-style modifiers;
- override sounds and UI textures;
- edit the mod's built-in cases.

> [!IMPORTANT]
> Lootbox registration runs during **KubeJS startup scripts**.  
> Put these scripts in `kubejs/startup_scripts/` and perform a **full game restart** after changing them. `/reload` is not enough.

---

## Contents

- [Requirements](#requirements)
- [Your first case](#your-first-case)
- [Automatic item registration](#automatic-item-registration)
- [Default resource paths](#default-resource-paths)
- [Registering and editing cases](#registering-and-editing-cases)
- [Case builder reference](#case-builder-reference)
- [Normal loot](#normal-loot)
- [Loot entry reference](#loot-entry-reference)
- [Using item tags](#using-item-tags)
- [Rarity tiers](#rarity-tiers)
- [StatTrak modifiers](#stattrak-modifiers)
- [Legendary rewards](#legendary-rewards)
- [How probabilities work](#how-probabilities-work)
- [Sounds](#sounds)
- [Custom UI textures](#custom-ui-textures)
- [Asset layout](#asset-layout)
- [Translations](#translations)
- [Editing the built-in case](#editing-the-built-in-case)
- [Common recipes](#common-recipes)
- [Troubleshooting](#troubleshooting)
- [Complete example](#complete-example)

---

# Requirements

This documentation targets:

- Minecraft `1.20.1`
- Minecraft Forge `47.x`
- Java `17`
- KubeJS
- GeckoLib
- GeckoMesh
- LDLib
- CS2 Lootbox

The CS2 Lootbox KubeJS event is:

```js
CS2LootboxEvents.register(event => {
    // Register or modify cases here.
})
```

The event is available only to **startup scripts**.

Recommended file:

```text
kubejs/startup_scripts/cs2lootbox_crates.js
```

---

# Your first case

A basic case can be created with only a few calls:

```js
CS2LootboxEvents.register(event => {
    event.addCrate('kubejs:my_case', crate => {
        crate.model('kubejs:geo/my_case.geo.json')
        crate.texture('kubejs:textures/lootbox/my_case.png')
        crate.animation('kubejs:animations/my_case.animation.json')
        crate.keyTexture('kubejs:item/my_case_key')

        crate.loot('minecraft:iron_ingot', 70)
        crate.loot('minecraft:diamond', 30)
    })
})
```

This automatically registers:

```text
kubejs:my_case_case
kubejs:my_case_key
```

The first item is the case and the second is its matching key.

You do **not** need a separate `ItemEvents.registry` block for them.

---

# Automatic item registration

By default, each lootbox definition creates two Minecraft items:

1. a case item;
2. a matching key item.

Free-to-open definitions create only the case item.

For:

```js
event.addCrate('kubejs:revolution', crate => {
    // ...
})
```

the default item IDs are:

```text
kubejs:revolution_case
kubejs:revolution_key
```

You can override them:

```js
crate.caseItem('kubejs:custom_case_item')
crate.keyItem('kubejs:custom_key_item')
```

The mod registers the final case/key `ItemBuilder`s **after all `CS2LootboxEvents.register` callbacks have finished**, which also allows `changeCase()` to modify a case before its items are finalized.

> [!WARNING]
> Do not separately register another item with the same ID through `ItemEvents.registry`.

---

## Free-to-open cases

Some CS2 drops, such as dossiers and sticker containers, do not need a key. Mark them free-to-open with either form:

```js
crate.requiresKey(false)
// or
crate.freeToOpen()
```

For a free-to-open case:

- no key item is registered;
- the opening screen shows **No key required / Free to open**;
- the server does not search for or consume a key;
- the case item itself is still consumed when the reward is accepted.

Keyed cases remain the default, so existing scripts do not need to change.


# Default resource paths

When you register:

```js
event.addCrate('kubejs:revolution', crate => {
})
```

CS2 Lootbox derives these defaults from the case ID:

| Resource | Default |
| --- | --- |
| Case item | `kubejs:revolution_case` |
| Key item | `kubejs:revolution_key` |
| GeckoLib model | `kubejs:geo/revolution.geo.json` |
| Case texture | `kubejs:textures/lootbox/revolution.png` |
| GeckoLib animation | `kubejs:animations/revolution.animation.json` |
| Key texture | `kubejs:item/revolution_key` |
| Case item display JSON | `cs2lootbox:item/lootbox_case` |

Therefore this is valid if your assets already follow the default names:

```js
CS2LootboxEvents.register(event => {
    event.addCrate('kubejs:revolution', crate => {
        crate.loot('minecraft:iron_ingot', 80)
        crate.loot('minecraft:diamond', 20)
    })
})
```

You can override any resource explicitly when needed.

---

# Registering and editing cases

## `event.addCrate(id, callback)`

Registers a new case.

```js
event.addCrate('kubejs:revolution', crate => {
    // configure crate
})
```

## `event.addCase(id, callback)`

Alias of `addCrate()`.

```js
event.addCase('kubejs:revolution', crate => {
    // configure crate
})
```

## `event.changeCase(id, callback)`

Edits a case that has already been registered.

The current values are copied first, so you only need to change the fields you want.

```js
event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
    crate.scale(340)
    crate.rotation(-3, 175, -3)
})
```

To replace an existing loot table:

```js
event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
    crate.clearLoot()

    crate.loot('minecraft:iron_ingot', 75)
    crate.loot('minecraft:diamond', 25)
})
```

To replace legendary rewards too:

```js
event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
    crate.clearLegendary()

    crate.legendary(0.50)
        .tooltip('★ Rare Special Item ★')
        .subLoot()
            .add('minecraft:nether_star', 100)
})
```

The case must already exist before `changeCase()` is called.

---

# Case builder reference

## Case and key resources

| Method | Description |
| --- | --- |
| `caseItem(id)` | Overrides the case item registry ID |
| `keyItem(id)` | Overrides the key item registry ID |
| `model(resource)` | GeckoLib `.geo.json` model |
| `texture(resource)` | Diffuse case texture |
| `animation(resource)` | GeckoLib `.animation.json` |
| `keyTexture(resource)` | 2D key item texture (used only when a key is required) |
| `requiresKey(boolean)` | Enables/disables matching-key registration and consumption; default `true` |
| `freeToOpen()` | Shortcut for `requiresKey(false)` |
| `noKeyRequired()` | Alias of `freeToOpen()` |
| `itemJson(resource)` | Parent Minecraft item-model JSON used for inventory/hand/fixed transforms |

Example:

```js
crate.caseItem('kubejs:revolution_case')
crate.keyItem('kubejs:revolution_key')

crate.model('kubejs:geo/revolution.geo.json')
crate.texture('kubejs:textures/lootbox/revolution.png')
crate.animation('kubejs:animations/revolution.animation.json')
crate.keyTexture('kubejs:item/revolution_key')

crate.itemJson('cs2lootbox:item/lootbox_case')
```

`itemJson()` controls the vanilla item display transforms. The actual animated case geometry is still rendered with GeckoLib.

---

## 3D case preview

```js
crate.position(22, 30)
crate.scale(78)
crate.rotation(0, 180, 0)
crate.light(12, 12)
```

| Method | Description |
| --- | --- |
| `position(x, y)` | X/Y model offset inside the case preview |
| `scale(value)` | Case model scale |
| `rotation(pitch, yaw, roll)` | Preview rotation in degrees |
| `light(blockLight, skyLight)` | Fixed GUI light values, each clamped to `0..15` |

The defaults are:

```text
position = 70, 150
scale    = 300
rotation = -3, 160, -3
light    = 12, 12
```

You will usually want to tune the transform for your own model.

---

## Animations

```js
crate.animations('fall', 'idle', 'open', 'open_idle')
crate.itemIdleAnimation('idle')
```

For a model without a separate `open_idle` animation:

```js
crate.animations('fall', 'idle', 'open')
```

The last frame of `open` is held while the UI waits for the carousel.

You can also explicitly clear or change open-idle:

```js
crate.openIdleAnimation(null)
crate.openIdleAnimation('open_idle')
```

`itemIdleAnimation()` controls the looping animation used while the case exists as an `ItemStack`.

---

## Stack sizes

```js
crate.caseStackSize(16)
crate.keyStackSize(64)
```

The default values are:

```text
case = 16
key  = 64
```

`keyStackSize()` is ignored for free-to-open cases because no key item is registered.

---

## Names

```js
crate.caseName('item.kubejs.revolution_case')
crate.keyName('item.kubejs.revolution_key')
```

Aliases also exist:

```js
crate.caseTranslationKey('item.kubejs.revolution_case')
crate.keyTranslationKey('item.kubejs.revolution_key')
```

If omitted, normal Minecraft item translation keys are used automatically.

---

## Collection information

The reward screen can display a collection name and icon:

```js
crate.collection(
    'collection.kubejs.revolution',
    'kubejs:textures/gui/collections/revolution.png'
)
```

Or configure them independently:

```js
crate.collectionText('collection.kubejs.revolution')
crate.collectionImage('kubejs:textures/gui/collections/revolution.png')
```

`collectionText()` accepts either a translation key or literal text.

Compatibility aliases are also available:

```js
crate.resultCollectionName('collection.kubejs.revolution')
crate.resultCollectionIcon('kubejs:textures/gui/collections/revolution.png')

crate.resultInfo('collection.kubejs.revolution')
crate.resultInfoIcon('kubejs:textures/gui/collections/revolution.png')
```

---

# Normal loot

There are several ways to add rewards.

## Simple reward

```js
crate.loot('minecraft:diamond', 10)
```

## Fixed count

Use `lootCount()` for the fixed-count overload because it avoids ambiguity with Rhino callbacks:

```js
crate.lootCount('minecraft:diamond', 10, 3)
```

## Random count

```js
crate.loot('minecraft:diamond', 10, 1, 4)
```

This rolls an inclusive count between `1` and `4`.

## Configured reward

```js
crate.loot('minecraft:diamond', 10, loot => {
    loot.count(1, 2)
    loot.name('loot.kubejs.diamond')
    loot.rarity('rarity.kubejs.classified', 0xD32EE6)
    loot.description('loot.kubejs.diamond.description')
})
```

## Remove rewards

```js
crate.removeLoot('minecraft:diamond')
crate.removeLoot('#forge:ingots/iron')
```

Remove the complete normal table:

```js
crate.clearLoot()
```

This is especially useful inside `changeCase()`.

---

# Loot entry reference

A configured entry receives a `LootEntryBuilder`.

## Stack count

```js
loot.count(3)
loot.count(1, 4)
```

Aliases:

```js
loot.stackSize(3)
loot.stackSize(1, 4)
```

The final amount is also capped to the actual selected item's maximum stack size.

---

## Custom reward name

```js
loot.name('loot.kubejs.special_diamond')
```

If omitted, Minecraft uses the normal `ItemStack` hover name.

---

## Rarity

```js
loot.rarity('rarity.kubejs.classified')
```

With a custom color:

```js
loot.rarity('rarity.kubejs.classified', 0xD32EE6)
```

Set only the color:

```js
loot.rarityColor(0xD32EE6)
```

Set the internal CS2 rarity tier explicitly:

```js
loot.rarityTier('classified')
```

---

## Description

Each call adds one translated line:

```js
loot.description('loot.kubejs.line_1')
loot.description('loot.kubejs.line_2')
```

---

## ItemStack NBT / SNBT

```js
loot.nbt('{CustomModelData:123}')
```

`tag()` is an alias:

```js
loot.tag('{CustomModelData:123}')
```

You can remove previously configured NBT while editing a copied entry definition:

```js
loot.clearNbt()
```

The configured NBT is applied consistently to:

- the case item list;
- carousel cards;
- the 3D inspector;
- the final server-authoritative reward.

Example enchanted book:

```js
crate.loot('minecraft:enchanted_book', 5, loot => {
    loot.nbt('{StoredEnchantments:[{id:"minecraft:sharpness",lvl:5s}]}')
    loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
})
```

---

## Per-item item-list transform

These transforms affect **only the item displayed in the case content list**.

```js
loot.position(2, -3)
loot.rotation(0, 25, -8)
loot.scale(1.15)
```

3D position:

```js
loot.position(2, -3, 10)
```

Independent scale:

```js
loot.scale(1.0, 1.2, 1.0)
```

Short aliases:

```js
loot.pos(2, -3)
loot.rot(0, 25, -8)
```

Everything in one call:

```js
loot.transform(
    2, -3, 0,    // position X, Y, Z
    0, 25, -8,   // rotation X, Y, Z
    1.15          // uniform scale
)
```

These transforms do **not** alter the actual awarded `ItemStack`.

---

# Using item tags

Prefix a reward source with `#` to use a Minecraft or Forge item tag:

```js
crate.loot('#forge:ingots/iron', 15, loot => {
    loot.count(1, 3)
    loot.rarity('rarity.kubejs.milspec', 0x4B69FF)
})
```

When the entry wins, the server randomly chooses one item from the tag.

For static previews such as the case contents list and inspector, CS2 Lootbox uses the first registry-sorted member of the tag so the preview stays deterministic.

NBT works with tag-backed entries too:

```js
crate.loot('#forge:ingots/iron', 15, loot => {
    loot.nbt('{CustomModelData:7}')
})
```

You can remove one with:

```js
crate.removeLoot('#forge:ingots/iron')
```

> [!NOTE]
> An empty or invalid tag cannot produce a valid reward. Make sure the tag contains registered items on both server and client.

---

# Rarity tiers

The internal rarity tier controls CS2-style presentation and rarity-specific sound selection.

Supported tiers are:

| Tier | Typical CS2 style |
| --- | --- |
| `consumer` | White / gray |
| `industrial` | Light blue |
| `milspec` | Blue |
| `restricted` | Purple |
| `classified` | Pink |
| `covert` | Red |
| `special` | Gold / rare special |

Example:

```js
loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
loot.rarityTier('covert')
```

In many cases `rarityTier()` is optional. The mod attempts to infer the tier from common words in the rarity translation key such as:

```text
consumer
industrial
milspec
mil_spec
restricted
classified
covert
special
gold
knife
glove
```

For unusual custom translation keys, explicitly call `rarityTier()` so the correct UI/sound tier is used.

---

# StatTrak modifiers

Each loot entry exposes:

```js
loot.modifiers
```

The currently supported modifier is:

```text
modifier.stat_track
```

## Basic StatTrak chance

```js
crate.loot('minecraft:diamond_sword', 10, loot => {
    loot.modifiers.add('modifier.stat_track', 15)
})
```

This gives the authoritative reward a `15%` chance to become StatTrak.

These aliases for the modifier ID are also accepted:

```text
modifier.stat_track
stat_track
stattrack
cs2lootbox:stat_track
```

## One-star chance

```js
loot.modifiers.add(
    'modifier.stat_track',
    15, // StatTrak chance
    10  // one-star chance if StatTrak succeeded
)
```

## Two-star chance

```js
loot.modifiers.add(
    'modifier.stat_track',
    15, // StatTrak chance
    10, // conditional one-star chance
    2   // conditional two-star chance after one-star succeeded
)
```

The order is:

```text
reward wins
    ↓
StatTrak roll
    ↓ success
one-star roll
    ↓ success
two-star roll
```

The two-star roll never occurs unless the one-star roll succeeded.

StatTrak mutation is performed only on the **server-selected final reward**, not on cosmetic carousel filler items.

Server configuration can disable StatTrak rolls or kill tracking globally.

---

# Legendary rewards

Legendary rewards are separate from the normal loot table.

```js
crate.legendary(0.26)
    .itemListName('★ Rare Special Item ★')
    .tooltip('★ Rare Special Item ★')
    .foreground('kubejs:textures/gui/my_rare_item.png')
    .subLoot()
        .add('minecraft:netherite_ingot', 60)
        .add('minecraft:nether_star', 40)
```

## First-stage chance

The number passed to:

```js
crate.legendary(0.26)
```

is an **absolute percentage**.

`0.26` means exactly:

```text
0.26% chance per opening
```

It is not a normal loot weight.

The backwards-compatible overload:

```js
crate.legendary()
```

uses `1.0%`, but explicitly specifying the chance is recommended.

---

## Legendary item-list name and tooltip

The gold legendary entry in the case contents `item_list` can have its own translated name:

```js
crate.legendary(0.26)
    .itemListName('legendary.kubejs.rare_gloves')
```

Short alias:

```js
crate.legendary(0.26)
    .name('legendary.kubejs.rare_gloves')
```

The case item's hover tooltip is configured separately:

```js
crate.legendary(0.26)
    .itemListName('legendary.kubejs.rare_gloves')
    .tooltip('tooltip.kubejs.rare_gloves')
```

Both methods accept either a translation key or literal text. If `itemListName(...)` is omitted, the `item_list` falls back to the `tooltip(...)` value for compatibility with older scripts.

The **primary legendary roulette card intentionally renders no text**. It only displays the gold card/foreground artwork, so long names cannot be truncated over the carousel image.

For example:

```json
{
  "legendary.kubejs.rare_gloves": "★ Rare Gloves ★",
  "tooltip.kubejs.rare_gloves": "★ Rare Gloves ★"
}
```

The first key appears under the legendary `item_list` entry; the second appears in the case-item tooltip. They may use the same translation if you want identical text.

`tooltipText(...)` is an alias for `tooltip(...)`.

```js
.tooltipText('tooltip.kubejs.rare_gloves')
```

If no custom item-list name is configured, the `item_list` falls back to `cs2lootbox.legendary.special_item`. If no custom tooltip is configured, the case hover tooltip falls back to `cs2lootbox.tooltip.rare_special`.

---

## Optional second legendary carousel

The legendary sub-loot carousel is enabled by default:

```js
crate.legendary(0.26)
    .subLootCarousel(true)
    .subLoot()
        .add('minecraft:netherite_ingot', 5)
        .add('minecraft:nether_star', 4)
```

To skip it:

```js
crate.legendary(0.26)
    .itemListName('legendary.kubejs.rare_gloves')
    .tooltip('tooltip.kubejs.rare_gloves')
    .subLootCarousel(false)
    .subLoot()
        .add('minecraft:netherite_ingot', 5)
        .add('minecraft:nether_star', 4)
```

When `false`, the **server still rolls the exact `subLoot()` reward at opening time**. The only difference is presentation: after the primary carousel lands on the gold legendary card, the second legendary-only carousel is skipped and the already-selected final item is revealed directly on the prize screen.

A second carousel is also skipped automatically when `subLoot()` contains only **one valid configured reward**, even if `subLootCarousel(true)` is enabled. There is nothing meaningful to roll when every outcome is the same configured entry.

The same optimization applies to the primary carousel: when the first-stage pool contains only **one valid configured result** in total, CS2 Lootbox skips the roulette and reveals the already-selected reward directly on the result panel.

Available method names:

```js
legendary.subLootCarousel(false) // recommended
legendary.subCarousel(false)     // alias
legendary.subListCarousel(false) // alias
```

This setting is per `legendary()` panel, so one case can mix panels that use a second carousel with panels that reveal immediately.

---

## Legendary foreground

```js
crate.legendary(0.26)
    .foreground('kubejs:textures/gui/my_rare_item.png')
```

If omitted, the mod uses its built-in:

```text
cs2lootbox:textures/gui/default_rare_item.png
```

---

## Legendary sub-loot

After a legendary panel wins, its own second-stage weighted table is used. This server-side roll happens regardless of whether the optional second visual carousel is enabled:

```js
crate.legendary(0.26)
    .subLoot()
        .add('minecraft:netherite_ingot', 5)
        .add('minecraft:nether_star', 4)
```

The sub-loot weights are relative to each other.

Here the final legendary distribution is:

```text
netherite_ingot = 5 / 9 = 55.56%
nether_star      = 4 / 9 = 44.44%
```

Fixed count:

```js
.subLoot()
    .addCount('minecraft:diamond', 10, 3)
```

Random count:

```js
.subLoot()
    .add('minecraft:diamond', 10, 1, 4)
```

Configured entry:

```js
.subLoot()
    .add('minecraft:nether_star', 10, loot => {
        loot.count(1)
        loot.name('loot.kubejs.nether_star')
        loot.rarity('rarity.kubejs.special', 0xFFAE39)
        loot.description('loot.kubejs.nether_star.description')
        loot.nbt('{CustomModelData:999}')
    })
```

Item tags are supported here as well:

```js
.subLoot()
    .add('#forge:gems/diamond', 10)
```

If no rarity tier is supplied for legendary sub-loot, CS2 Lootbox defaults it to `covert`.

---

## Multiple legendary panels

Multiple legendary panels are supported:

```js
crate.legendary(0.26)
    .itemListName('★ Rare Gloves ★')
    .tooltip('★ Rare Gloves ★')
    .subLoot()
        .add('minecraft:leather_boots', 100)

crate.legendary(0.10)
    .itemListName('★ Rare Knife ★')
    .tooltip('★ Rare Knife ★')
    .subLoot()
        .add('minecraft:netherite_sword', 100)
```

Each panel occupies its own absolute first-stage chance band.

The combined legendary chance cannot exceed `100%`.

Legendary panels are displayed after the normal entries in declaration order.

Remove all copied/configured legendary panels with:

```js
crate.clearLegendary()
```

Alias:

```js
crate.clearLegendaryLoot()
```

---

# How probabilities work

Normal loot and legendary loot intentionally use different probability systems.

## Normal loot

Normal entries are relative weights.

```js
crate.loot('minecraft:iron_ingot', 70)
crate.loot('minecraft:diamond', 30)
```

Inside the normal table:

```text
Iron    = 70%
Diamond = 30%
```

They do not need to add to `100`.

This works identically:

```js
crate.loot('minecraft:iron_ingot', 7)
crate.loot('minecraft:diamond', 3)
```

---

## Legendary first-stage chance

Legendary chance is checked **before** the normal table.

```js
crate.legendary(0.26)
```

means the opening has an absolute `0.26%` chance to enter that legendary stage.

If the legendary check misses, the normal table is rolled.

For one legendary panel with chance `L`, the overall chance of a normal entry is approximately:

```text
(1 - L / 100) × entryWeight / totalNormalWeight
```

Example:

```text
Legendary absolute chance = 0.26%
Normal:
    Iron    weight 70
    Diamond weight 30
```

Overall probabilities are approximately:

```text
Legendary = 0.26%
Iron      = 99.74% × 70% = 69.818%
Diamond   = 99.74% × 30% = 29.922%
```

---

## Legendary second-stage weights

After the legendary stage has already won:

```js
.subLoot()
    .add('minecraft:netherite_ingot', 5)
    .add('minecraft:nether_star', 4)
```

uses only:

```text
5 + 4 = 9
```

as its internal total.

The normal loot weights have no effect on this stage.

---

# Sounds

CS2 Lootbox provides built-in CS-style sounds, so custom sound configuration is optional.

## Default sound tuning

```js
crate.defaultSound(0.60, 1.0)
```

Or independently:

```js
crate.defaultSoundVolume(0.60)
crate.defaultSoundPitch(1.0)
```

Volume is clamped to the supported `0.0 .. 1.0` range and pitch to the supported sound tuning range.

---

## Opening and carousel

```js
crate.openSound('minecraft:block.chest.open')
crate.carouselTickSound('cs2lootbox:csgo_ui_crate_item_scroll')
```

With explicit volume/pitch:

```js
crate.openSound('minecraft:block.chest.open', 0.5, 1.0)
crate.carouselTickSound('cs2lootbox:csgo_ui_crate_item_scroll', 0.3, 1.0)
```

---

## Reward sounds by rarity

Fallback for lower tiers:

```js
crate.rewardSound('namespace:sound')
```

Rarity-specific overrides:

```js
crate.uncommonSound('namespace:sound')
crate.rareSound('namespace:sound')
crate.mythicalSound('namespace:sound')
crate.classifiedSound('namespace:sound')
crate.covertSound('namespace:sound')
crate.specialSound('namespace:sound')
```

Every one also has a `(sound, volume, pitch)` overload:

```js
crate.covertSound('namespace:sound', 0.8, 1.0)
```

---

# Custom UI textures

A case can override the roulette UI skin:

```js
crate.slotBorderTexture(
    'kubejs:textures/gui/slot_border.png'
)

crate.markerBarTexture(
    'kubejs:textures/gui/marker_bar.png'
)

crate.markerOuterCircleTexture(
    'kubejs:textures/gui/marker_outer_circle.png'
)

crate.markerInnerCircleTexture(
    'kubejs:textures/gui/marker_inner_circle.png'
)
```

If omitted, CS2 Lootbox uses its bundled textures.

---

# Asset layout

A typical KubeJS case can use this layout:

```text
kubejs/
├─ startup_scripts/
│  └─ cs2lootbox_crates.js
│
└─ assets/
   └─ kubejs/
      ├─ geo/
      │  └─ revolution.geo.json
      │
      ├─ animations/
      │  └─ revolution.animation.json
      │
      ├─ textures/
      │  ├─ lootbox/
      │  │  └─ revolution.png
      │  │
      │  ├─ item/
      │  │  └─ revolution_key.png
      │  │
      │  └─ gui/
      │     ├─ revolution_rare_item.png
      │     └─ collections/
      │        └─ revolution.png
      │
      └─ lang/
         ├─ en_us.json
         └─ fr_fr.json
```

For a definition named:

```text
kubejs:revolution
```

the default model resources already point to:

```text
kubejs:geo/revolution.geo.json
kubejs:textures/lootbox/revolution.png
kubejs:animations/revolution.animation.json
kubejs:item/revolution_key
```

---

# Translations

Example:

```json
{
  "item.kubejs.revolution_case": "Revolution Case",
  "item.kubejs.revolution_key": "Revolution Case Key",

  "collection.kubejs.revolution": "The Revolution Collection",

  "rarity.kubejs.industrial": "Industrial Grade",
  "rarity.kubejs.milspec": "Mil-Spec Grade",
  "rarity.kubejs.restricted": "Restricted",
  "rarity.kubejs.classified": "Classified",
  "rarity.kubejs.covert": "Covert",
  "rarity.kubejs.special": "★ Rare Special Item ★",

  "loot.kubejs.iron": "Iron Ingot",
  "loot.kubejs.iron.description": "A common reward from the Revolution Case.",

  "loot.kubejs.diamond": "Diamond",
  "loot.kubejs.diamond.description": "A valuable high-tier reward.",

  "tooltip.kubejs.rare_item": "★ Rare Special Item ★"
}
```

Place English translations in:

```text
kubejs/assets/kubejs/lang/en_us.json
```

French example:

```text
kubejs/assets/kubejs/lang/fr_fr.json
```

---

# Editing the built-in case

The built-in weapon case currently uses:

```text
cs2lootbox:csgo_case_weapon
```

You can modify only its presentation:

```js
CS2LootboxEvents.register(event => {
    event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
        crate.position(50, 120)
        crate.scale(320)
        crate.rotation(-3, 180, -3)
    })
})
```

Or replace the loot completely:

```js
CS2LootboxEvents.register(event => {
    event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
        crate.clearLoot()
        crate.clearLegendary()

        crate.loot('minecraft:iron_ingot', 80, loot => {
            loot.rarity('cs2lootbox.rarity.milspec', 0x4B69FF)
        })

        crate.loot('minecraft:diamond', 20, loot => {
            loot.rarity('cs2lootbox.rarity.classified', 0xD32EE6)
        })

        crate.legendary(0.26)
            .tooltip('★ Rare Special Item ★')
            .subLoot()
                .add('minecraft:nether_star', 100, loot => {
                    loot.rarity('cs2lootbox.rarity.special', 0xFFAE39)
                })
    })
})
```

Because `changeCase()` starts from a copy of the existing definition, forgetting `clearLoot()` will keep the old entries and append your new ones.

---

# Common recipes

## CustomModelData reward

```js
crate.loot('minecraft:diamond_sword', 10, loot => {
    loot.nbt('{CustomModelData:123}')
    loot.name('loot.kubejs.custom_sword')
    loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
})
```

---

## Enchanted reward

```js
crate.loot('minecraft:enchanted_book', 5, loot => {
    loot.nbt('{StoredEnchantments:[{id:"minecraft:sharpness",lvl:5s}]}')
    loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
})
```

---

## Tag-based material reward

```js
crate.loot('#forge:ingots/iron', 20, loot => {
    loot.count(2, 6)
    loot.rarity('rarity.kubejs.industrial', 0x5E98D9)
})
```

---

## StatTrak weapon

```js
crate.loot('minecraft:netherite_sword', 5, loot => {
    loot.name('loot.kubejs.special_sword')
    loot.rarity('rarity.kubejs.covert', 0xEB4B4B)

    loot.modifiers.add(
        'modifier.stat_track',
        10, // 10% StatTrak
        5,  // 5% one-star after StatTrak
        1   // 1% two-star after one-star
    )
})
```

---

## Multiple description lines

```js
crate.loot('minecraft:diamond', 10, loot => {
    loot.description('loot.kubejs.diamond.line1')
    loot.description('loot.kubejs.diamond.line2')
})
```

---

## Case with no custom sounds

You do not have to define sounds:

```js
event.addCrate('kubejs:simple_case', crate => {
    crate.loot('minecraft:iron_ingot', 90)
    crate.loot('minecraft:diamond', 10)
})
```

The bundled CS2 Lootbox sounds are used automatically.

---

# Troubleshooting

## My script does not run after `/reload`

Use a full restart.

`CS2LootboxEvents.register` is a **startup event**, because it creates Minecraft registry items.

---

## The case or key is missing

Check that your script is inside:

```text
kubejs/startup_scripts/
```

not:

```text
kubejs/server_scripts/
kubejs/client_scripts/
```

Also check the game log for a KubeJS startup error. One invalid lootbox can fail while its definition is being built.

---

## I registered the case/key with `ItemEvents.registry` too

Remove the duplicate item registration.

CS2 Lootbox creates the case and key items itself from the lootbox definition.

---

## My custom model is missing

For:

```js
crate.model('kubejs:geo/revolution.geo.json')
```

the file must be:

```text
kubejs/assets/kubejs/geo/revolution.geo.json
```

Likewise:

```js
crate.texture('kubejs:textures/lootbox/revolution.png')
```

expects:

```text
kubejs/assets/kubejs/textures/lootbox/revolution.png
```

---

## My key texture is missing

`keyTexture()` uses normal item texture syntax:

```js
crate.keyTexture('kubejs:item/revolution_key')
```

which points to:

```text
kubejs/assets/kubejs/textures/item/revolution_key.png
```

Do not write:

```text
kubejs:textures/item/revolution_key.png
```

inside `keyTexture()`.

---

## The case model is too large or misplaced

Tune:

```js
crate.position(x, y)
crate.scale(value)
crate.rotation(pitch, yaw, roll)
```

These settings control the large 3D case preview.

---

## One reward looks wrong in the item list

Give only that reward an item-list transform:

```js
crate.loot('namespace:item', 10, loot => {
    loot.position(2, -4)
    loot.rotation(0, 20, 0)
    loot.scale(0.85)
})
```

This does not change the awarded item.

---

## My rarity has the wrong sound/effect

Explicitly provide the internal tier:

```js
loot.rarity('my.custom.rarity', 0xEB4B4B)
loot.rarityTier('covert')
```

Automatic tier detection is based on common words in the translation key.

---

## `changeCase()` says the case does not exist

The target must have been registered earlier in startup processing.

The built-in case is registered before KubeJS callbacks, so this works:

```js
event.changeCase('cs2lootbox:csgo_case_weapon', crate => {
})
```

For custom cases, make sure the `addCrate()` call runs before the corresponding `changeCase()` call.

---

## Legendary chance looks different from normal weights

That is intentional.

```js
crate.legendary(0.26)
```

means **0.26% absolute chance**, while:

```js
crate.loot('minecraft:diamond', 30)
```

means a relative weight inside the normal table.

---

## My legendary table throws an error

Every legendary panel needs at least one sub-loot entry with a positive weight:

```js
crate.legendary(0.26)
    .subLoot()
        .add('minecraft:nether_star', 100)
```

The combined absolute chance of all legendary panels must also stay at or below `100%`.

---

# Complete example

```js
// kubejs/startup_scripts/cs2lootbox_crates.js
//
// CS2 Lootbox definitions are startup-only.
// Restart Minecraft after changing this file.

CS2LootboxEvents.register(event => {
    event.addCrate('kubejs:revolution', crate => {
        // -------------------------------------------------------------
        // ITEMS / MODEL
        // -------------------------------------------------------------
        crate.caseItem('kubejs:revolution_case')
        crate.keyItem('kubejs:revolution_key')

        crate.model('kubejs:geo/revolution.geo.json')
        crate.texture('kubejs:textures/lootbox/revolution.png')
        crate.animation('kubejs:animations/revolution.animation.json')
        crate.keyTexture('kubejs:item/revolution_key')

        // Shared vanilla item-display transform JSON.
        crate.itemJson('cs2lootbox:item/lootbox_case')

        // -------------------------------------------------------------
        // 3D CASE PRESENTATION
        // -------------------------------------------------------------
        crate.position(22, 30)
        crate.scale(78)
        crate.rotation(0, 180, 0)
        crate.light(12, 12)

        crate.animations('fall', 'idle', 'open', 'open_idle')
        crate.itemIdleAnimation('idle')

        // -------------------------------------------------------------
        // ITEM SETTINGS / TEXT
        // -------------------------------------------------------------
        crate.caseStackSize(16)
        crate.keyStackSize(64)

        crate.caseName('item.kubejs.revolution_case')
        crate.keyName('item.kubejs.revolution_key')

        crate.collection(
            'collection.kubejs.revolution',
            'kubejs:textures/gui/collections/revolution.png'
        )

        // -------------------------------------------------------------
        // OPTIONAL SOUND OVERRIDES
        // -------------------------------------------------------------
        crate.defaultSound(0.60, 1.0)

        // Bundled sounds are already used by default, so these are optional:
        // crate.openSound('cs2lootbox:case_unlock', 0.2, 1.0)
        // crate.carouselTickSound('cs2lootbox:csgo_ui_crate_item_scroll')

        // -------------------------------------------------------------
        // NORMAL LOOT
        // -------------------------------------------------------------
        crate.loot('minecraft:iron_ingot', 55, loot => {
            loot.count(1, 4)
            loot.name('loot.kubejs.iron')
            loot.rarity('rarity.kubejs.industrial', 0x5E98D9)
            loot.rarityTier('industrial')
            loot.description('loot.kubejs.iron.description')
        })

        crate.loot('minecraft:gold_ingot', 25, loot => {
            loot.count(1, 3)
            loot.rarity('rarity.kubejs.milspec', 0x4B69FF)
            loot.rarityTier('milspec')
        })

        crate.loot('minecraft:diamond', 12, loot => {
            loot.count(1, 2)
            loot.name('loot.kubejs.diamond')
            loot.rarity('rarity.kubejs.classified', 0xD32EE6)
            loot.rarityTier('classified')
            loot.description('loot.kubejs.diamond.description')

            // Only affects this item's card in the case contents list.
            loot.position(2, -3)
            loot.rotation(0, 25, -8)
            loot.scale(1.15)

            // Applied to preview + final server reward.
            loot.nbt('{CustomModelData:123}')
        })

        crate.loot('minecraft:netherite_sword', 8, loot => {
            loot.count(1)
            loot.name('loot.kubejs.netherite_sword')
            loot.rarity('rarity.kubejs.covert', 0xEB4B4B)
            loot.rarityTier('covert')

            // 10% StatTrak chance.
            loot.modifiers.add('modifier.stat_track', 10)
        })

        // One weighted entry backed by an item tag.
        crate.loot('#forge:ingots/iron', 5, loot => {
            loot.count(1, 3)
            loot.rarity('rarity.kubejs.milspec', 0x4B69FF)
            loot.rarityTier('milspec')
        })

        // -------------------------------------------------------------
        // LEGENDARY STAGE
        // -------------------------------------------------------------
        // Absolute 0.26% first-stage chance.
        crate.legendary(0.26)
            .itemListName('tooltip.kubejs.rare_item')
            .tooltip('tooltip.kubejs.rare_item')
            .foreground('kubejs:textures/gui/revolution_rare_item.png')
            // true by default. Set false to reveal the final sub-loot item
            // directly after the primary gold card instead of a second carousel.
            .subLootCarousel(false)
            .subLoot()
                .add('minecraft:netherite_ingot', 5, loot => {
                    loot.count(1)
                    loot.name('loot.kubejs.netherite')
                    loot.rarity('rarity.kubejs.special', 0xFFAE39)
                    loot.rarityTier('special')
                    loot.description('loot.kubejs.netherite.description')
                })
                .add('minecraft:nether_star', 4, loot => {
                    loot.count(1)
                    loot.name('loot.kubejs.nether_star')
                    loot.rarity('rarity.kubejs.special', 0xFFAE39)
                    loot.rarityTier('special')
                    loot.description('loot.kubejs.nether_star.description')
                })
    })
})
```

A matching translation file could be:

```json
{
  "item.kubejs.revolution_case": "Revolution Case",
  "item.kubejs.revolution_key": "Revolution Case Key",

  "collection.kubejs.revolution": "The Revolution Collection",

  "rarity.kubejs.industrial": "Industrial Grade",
  "rarity.kubejs.milspec": "Mil-Spec Grade",
  "rarity.kubejs.classified": "Classified",
  "rarity.kubejs.covert": "Covert",
  "rarity.kubejs.special": "★ Rare Special Item ★",

  "loot.kubejs.iron": "Iron Ingot",
  "loot.kubejs.iron.description": "A common reward from the Revolution Case.",

  "loot.kubejs.diamond": "Diamond",
  "loot.kubejs.diamond.description": "A valuable high-tier reward.",

  "loot.kubejs.netherite_sword": "Netherite Sword",

  "loot.kubejs.netherite": "Netherite Ingot",
  "loot.kubejs.netherite.description": "An exceedingly rare reward.",

  "loot.kubejs.nether_star": "Nether Star",
  "loot.kubejs.nether_star.description": "One of the rarest rewards in the case.",

  "tooltip.kubejs.rare_item": "★ Rare Special Item ★"
}
```

---

## Included example files

The mod source also contains example KubeJS content under:

```text
examples/kubejs/
```

including:

```text
examples/kubejs/startup_scripts/cs2lootbox_crates.js
examples/kubejs/startup_scripts/cs2lootbox_rarity_example.js
examples/kubejs/assets/kubejs/lang/en_us.json
examples/kubejs/assets/kubejs/lang/fr_fr.json
```

These are useful as copy/paste starting points for modpack development.
