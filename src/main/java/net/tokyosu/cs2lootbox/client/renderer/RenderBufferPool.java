package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.ArrayDeque;

/** Render-thread scratch buffers. Minecraft's raw native allocations have no GC cleanup. */
public final class RenderBufferPool {
    private static final ArrayDeque<Builder> AVAILABLE = new ArrayDeque<>();
    private static int created, borrowed, peakBorrowed;
    private RenderBufferPool() { }

    public static Builder acquire() {
        Builder builder = AVAILABLE.pollFirst();
        if (builder == null) {
            builder = new Builder();
            created++;
        }
        builder.borrowed = true;
        borrowed++;
        peakBorrowed = Math.max(peakBorrowed, borrowed);
        return builder;
    }

    public record Statistics(int created, int available, int borrowed, int peakBorrowed) { }
    public static Statistics statistics() {
        return new Statistics(created, AVAILABLE.size(), borrowed, peakBorrowed);
    }

    public static final class Builder extends BufferBuilder implements AutoCloseable {
        private boolean borrowed;
        private Builder() { super(4096); }

        @Override public void close() {
            if (!borrowed) return;
            // Failure paths may leave a draw open. Finish/release it before reuse.
            if (building()) {
                RenderedBuffer data = endOrDiscardIfEmpty();
                if (data != null) data.release();
            }
            discard();
            borrowed = false;
            RenderBufferPool.borrowed--;
            // Keep the allocation alive and reuse it: discard() does not free it.
            AVAILABLE.addFirst(this);
        }
    }
}
