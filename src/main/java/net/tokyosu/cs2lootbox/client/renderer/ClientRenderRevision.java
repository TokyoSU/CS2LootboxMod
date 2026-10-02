package net.tokyosu.cs2lootbox.client.renderer;

/** Resource/language reload epoch used by lightweight, screen-owned caches. */
public final class ClientRenderRevision {
    private static volatile long revision;
    private ClientRenderRevision() {}
    public static long get() { return revision; }
    public static void invalidate() { revision++; }
}
