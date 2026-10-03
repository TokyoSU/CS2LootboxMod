package net.tokyosu.cs2lootbox.client.renderer;

import net.minecraft.client.renderer.ShaderInstance;

/** Texture/color shader which retains faint blur taps instead of discarding alpha below 0.1. */
public final class GuiArtworkShader {
    private static ShaderInstance shader, premultiplied;
    private GuiArtworkShader() {}
    public static ShaderInstance get() { return shader; }
    public static ShaderInstance getPremultiplied() { return premultiplied; }
    public static void setPremultiplied(ShaderInstance loaded) { premultiplied = loaded; }
    public static void set(ShaderInstance loaded) { shader = loaded; }
}
