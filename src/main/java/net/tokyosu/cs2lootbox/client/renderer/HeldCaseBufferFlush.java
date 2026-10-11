package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.tokyosu.cs2lootbox.mixin.client.BufferSourceAccessor;

import java.util.Map;
import java.util.Set;

/** Preserve batch ordering/transparency, but do not end every empty fixed buffer. */
final class HeldCaseBufferFlush {
    private HeldCaseBufferFlush() {}

    static boolean flush(MultiBufferSource.BufferSource source) {
        // A renderer mod's subclass may have additional queues/flush semantics.
        // Use the public full flush there, or if the optional accessor is absent.
        if (source.getClass() != MultiBufferSource.BufferSource.class || !(source instanceof BufferSourceAccessor access)) {
            source.endBatch();
            return false;
        }
        flushStarted(source, access.cs2lootbox$getFixedBuffers(), access.cs2lootbox$getStartedBuffers());
        return true;
    }

    static void flushStarted(MultiBufferSource.BufferSource source,
                             Map<RenderType, BufferBuilder> fixed, Set<BufferBuilder> started) {
        source.endLastBatch();
        if (started.isEmpty()) return;
        // Iteration order is the vanilla fixed-buffer order; never reorder alpha
        // blending or leave a prior shield/glint draw pending behind the case.
        for (var entry : fixed.entrySet()) {
            if (started.contains(entry.getValue())) source.endBatch(entry.getKey());
            if (started.isEmpty()) break;
        }
    }
}
