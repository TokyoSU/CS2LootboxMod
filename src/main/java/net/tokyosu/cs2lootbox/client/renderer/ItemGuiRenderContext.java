package net.tokyosu.cs2lootbox.client.renderer;

import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;

/** Scoped GUI purpose; always restored after an item draw, including failed draws. */
public final class ItemGuiRenderContext {
    public enum Purpose { INVENTORY, CAROUSEL, REVEAL }
    private static final ThreadLocal<Purpose> CURRENT = ThreadLocal.withInitial(() -> Purpose.INVENTORY);
    private ItemGuiRenderContext() {}

    public static Purpose currentPurpose() { return CURRENT.get(); }

    public static void render(Purpose purpose, Runnable draw) {
        Purpose previous = CURRENT.get();
        CURRENT.set(purpose);
        try { draw.run(); }
        finally { CURRENT.set(previous); }
    }

    public static LootboxDefinition.ItemTransform select(LootboxDefinition.ItemDisplayTransforms transforms) {
        LootboxDefinition.ItemTransform override = switch (CURRENT.get()) {
            case CAROUSEL -> transforms.guiCarousel();
            case REVEAL -> transforms.guiReveal();
            default -> null;
        };
        return override == null ? transforms.gui() : override;
    }
}
