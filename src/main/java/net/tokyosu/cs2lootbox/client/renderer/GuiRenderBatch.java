package net.tokyosu.cs2lootbox.client.renderer;

import net.minecraft.client.gui.GuiGraphics;

/** Coalesce consecutive 2D fills without crossing item, texture or depth boundaries. */
public final class GuiRenderBatch {
    private GuiRenderBatch() {}

    @SuppressWarnings("deprecation")
    public static void fills(GuiGraphics graphics, Runnable draw) {
        // 1.20.1's public batching API is deprecated, but ordinary fill otherwise
        // flushes after every rectangle. Scope it narrowly and recover its flag
        // on failure because vanilla drawManaged does not use a finally block.
        try { graphics.drawManaged(draw); }
        catch (RuntimeException | Error failure) {
            try { graphics.drawManaged(() -> {}); }
            catch (RuntimeException | Error recovery) { failure.addSuppressed(recovery); }
            throw failure;
        }
    }
}
