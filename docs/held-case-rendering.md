# Held-case rendering optimisation

Constant first-person case poses reuse GeckoLib's evaluated bone transforms
after at least one tick of stability. Each renderer retains only its own current
item-stack pose. The cache restores shared bones if an inventory or opening
renderer changed them; a new stack, model, resource revision or clock rewind
requires live evaluation again.

The fast path requires a rigid model, a constant numeric idle clip, no render
layers and one running, untriggered `item` controller playing that idle in a loop.
Animated keyframes, expressions, events, transitions and weighted skins use the
normal rendering path. GeckoLib's pre/post render events and animated texture
updates remain active. Item transforms, full-detail geometry and texture images
are unchanged by this optimisation.

A client-only accessor mixin exposes the standard buffer source's pending-buffer
set. Before the cached GPU draw, the renderer ends its shared batch and only
pending fixed batches, in vanilla order. This skips empty buffers without moving
translucent shield/glint geometry behind the case. Custom buffer-source subclasses
and an unavailable accessor retain the public full-flush fallback.

## Verification

On Windows with the cached dependencies, run:

```powershell
./gradlew.bat heldCaseCacheCheck renderCommandCheck --offline
```

The hidden-window check exercises GPU cache reuse, pose restoration after shared
bone changes, stack/model/reload invalidation, static-clip eligibility and actual
vanilla buffer ordering. It is not an end-to-end Minecraft FPS benchmark.

In Creative mode, hold the case until equipping finishes, run
`/cs2lootbox renderstats reset`, wait about ten seconds and run
`/cs2lootbox renderstats`. A settled static case should show increasing `pose reuse`
and GPU cache hits, with few `pose evaluations`. Standard buffer sources should
also show `selective flushes`; custom rendering integrations may show `full flushes`.
Compare empty-hand and held-case FPS at the same location and graphics settings
on the affected machine. These counters establish path usage, not a percentage
FPS improvement.

These rendering improvements are included in the current `1.1.0` sources.
