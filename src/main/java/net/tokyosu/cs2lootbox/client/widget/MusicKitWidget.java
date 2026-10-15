package net.tokyosu.cs2lootbox.client.widget;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.tokyosu.cs2lootbox.client.ui.ClientUiActions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tokyosu.apocalypselib.utils.ModUtils;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.client.music.MusicKitPreview;
import net.tokyosu.cs2lootbox.client.music.ClientMusicKitAvailability;
import net.tokyosu.cs2lootbox.music.MusicKitSelection;
import net.tokyosu.cs2lootbox.ui.MusicKitUI;
import org.lwjgl.glfw.GLFW;
import org.joml.Vector3f;

/** CS2-inspired 2D player. Only Save crosses the network; previews never alter the held item. */
public final class MusicKitWidget extends Widget {
    private static final int SAVE = 1, REJECTED = 2;
    private static final int LIST_X = 64, LIST_Y = 114, LIST_W = 288, ROW_H = 38, VISIBLE = 7;
    private static final int LIST_H = ROW_H * VISIBLE, BAR_X = 357, BAR_W = 5;
    private static final int PLAYER_X = 390, PLAYER_W = 346, CONTROLS_Y = 392;
    private static final int TEXT = 0xFFEDEDEB, MUTED = 0xFFAFAFAB, BLUE = 0xFF9BCFE6;
    private static final int ORANGE = 0xFFDB8A2E, GREEN = 0xFF447956;
    private final MusicKitDefinition kit;
    private int selected, scroll;
    private boolean draggingScrollbar, waitingForSave;
    @OnlyIn(Dist.CLIENT)
    private MusicKitPreview preview;
    private boolean artworkChecked, artworkAvailable;
    private Component error;

    public MusicKitWidget(MusicKitDefinition kit, int selected) {
        super(0, 0, MusicKitUI.WIDTH, MusicKitUI.HEIGHT);
        this.kit = kit;
        this.selected = Math.max(0, Math.min(selected, kit.tracks().size() - 1));
        revealSelection();
    }

    @Override public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id != SAVE) { super.handleClientAction(id, buffer); return; }
        int track = buffer.readVarInt();
        if (!(getGui().entityPlayer instanceof ServerPlayer player)
                || !(getGui().holder instanceof HeldItemUIFactory.HeldItemHolder holder)
                || player.containerMenu != getGui().getModularUIContainer()) return;
        var held = player.getItemInHand(holder.getHand());
        var saved = held == holder.getHeld() ? MusicKitSelection.savedCopy(held, kit, track) : ItemStack.EMPTY;
        if (saved.isEmpty()) { writeUpdateInfo(REJECTED, out -> {}); return; }
        player.setItemInHand(holder.getHand(), saved);
        player.getInventory().setChanged();
        player.closeContainer();
    }

    @Override @OnlyIn(Dist.CLIENT) public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == REJECTED) {
            waitingForSave = false;
            error = Component.translatable("cs2lootbox.music.invalid_item");
        } else super.readUpdateInfo(id, buffer);
    }

    @OnlyIn(Dist.CLIENT) private MusicKitPreview preview() {
        if (preview == null) preview = new MusicKitPreview();
        return preview;
    }

    @OnlyIn(Dist.CLIENT) public void closePreview() { if (preview != null) preview.close(); }

    @SuppressWarnings("resource")
    @Override @OnlyIn(Dist.CLIENT) public void updateScreen() {
        super.updateScreen();
        if (!ClientMusicKitAvailability.isAvailable(kit)) {
            ClientUiActions.showActionBar(Component.translatable("cs2lootbox.music.missing_resources"));
            closeScreen();
            return;
        }
        if (preview != null) preview.tick();
    }
    
    @Override @OnlyIn(Dist.CLIENT) public void drawInBackground(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        if (!ClientMusicKitAvailability.isAvailable(kit)) return;
        super.drawInBackground(g, mouseX, mouseY, partialTicks);
        Font font = ModUtils.getFont();
        g.pose().pushPose();
        g.pose().translate(getPositionX(), getPositionY(), 0);
        mouseX -= getPositionX(); mouseY -= getPositionY();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            g.fill(44, 28, 764, 460, 0x50000000);
            g.fillGradient(40, 24, 760, 456, 0xDC515550, 0xE144413B);
            g.fill(40, 24, 760, 77, 0x90303538);
            g.fill(40, 77, 760, 78, 0x40767D7B);
            g.fill(40, 430, 760, 456, 0x50303435);
            label(g, Component.translatable("cs2lootbox.music.header"), 64, 42, 1.45F, TEXT);
            label(g, fit(kit.nameComponent(), 426, 1), 245, 48, 1, BLUE);
            button(g, 712, 38, 24, 24, mouseX, mouseY, false, true);
            g.drawCenteredString(font, "x", 724, 46, MUTED);
            g.drawString(font, Component.translatable("cs2lootbox.music.tracks", kit.tracks().size()), 64, 94, MUTED, false);
            g.drawString(font, Component.translatable("cs2lootbox.music.preview"), PLAYER_X, 94, MUTED, false);
            drawList(g, mouseX, mouseY);
            drawArtwork(g);
            var track = kit.tracks().get(selected);
            g.drawString(font, status(), PLAYER_X, 309, BLUE, false);
            label(g, fit(track.titleComponent(), PLAYER_W, 1.2F), PLAYER_X, 327, 1.2F, TEXT);
            double elapsed = preview == null ? 0 : preview.seconds();
            g.fill(PLAYER_X, 352, PLAYER_X + PLAYER_W, 359, 0xFF202D38);
            int progress = (int) Math.round(PLAYER_W * Math.min(1, elapsed / track.seconds()));
            if (progress > 0) g.fill(PLAYER_X, 352, PLAYER_X + progress, 359, 0xFF80BFD9);
            g.drawString(font, time(elapsed), PLAYER_X, 367, MUTED, false);
            String duration = time(track.seconds());
            g.drawString(font, duration, PLAYER_X + PLAYER_W - font.width(duration), 367, MUTED, false);
            drawControls(g, mouseX, mouseY);
            Component footer = error != null ? error : Component.translatable(waitingForSave
                    ? "cs2lootbox.music.saving" : "cs2lootbox.music.save_hint");
            g.drawString(font, fit(footer, 668, 1), 64, 439, error == null ? MUTED : 0xFFFFB4A3, false);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.pose().popPose();
        }
    }

    @OnlyIn(Dist.CLIENT) private void drawList(GuiGraphics g, int mx, int my) {
        Font font = ModUtils.getFont();
        g.fill(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H, 0xA52B2D2C);
        for (int row = 0; row < VISIBLE && scroll + row < kit.tracks().size(); row++) {
            int index = scroll + row, y = LIST_Y + row * ROW_H;
            boolean active = index == selected;
            boolean hover = inside(mx, my, LIST_X, y, LIST_W, ROW_H);
            g.fill(LIST_X, y, LIST_X + LIST_W, y + ROW_H - 1,
                    active ? 0xB0496678 : hover ? 0x805D6260 : (row % 2 == 0 ? 0x183F4442 : 0x083F4442));
            if (active) g.fill(LIST_X, y, LIST_X + 3, y + ROW_H - 1, BLUE);
            g.drawString(font, String.format(java.util.Locale.ROOT, "%02d", index + 1), LIST_X + 12, y + 14, active ? BLUE : MUTED, false);
            g.drawString(font, fit(kit.tracks().get(index).titleComponent(), 179, 1), LIST_X + 39, y + 14, TEXT, false);
            String duration = time(kit.tracks().get(index).seconds());
            g.drawString(font, duration, LIST_X + LIST_W - 10 - font.width(duration), y + 14, MUTED, false);
        }
        g.fill(BAR_X, LIST_Y, BAR_X + BAR_W, LIST_Y + LIST_H, 0x40312C24);
        if (maxScroll() > 0) g.fill(BAR_X, thumbY(), BAR_X + BAR_W, thumbY() + thumbHeight(), ORANGE);
    }

    @OnlyIn(Dist.CLIENT) private void drawArtwork(GuiGraphics g) {
        if (!artworkChecked) {
            artworkAvailable = kit.image() != null && Minecraft.getInstance().getResourceManager().getResource(kit.image()).isPresent();
            artworkChecked = true;
        }
        g.fillGradient(PLAYER_X, 114, PLAYER_X + PLAYER_W, 298, 0x303A4144, 0x184C4C46);
        if (artworkAvailable) {
            float fit = Math.min((PLAYER_W - 18F) / kit.imageWidth(), 176F / kit.imageHeight()) * kit.imageScale();
            int width = Math.round(kit.imageWidth() * fit), height = Math.round(kit.imageHeight() * fit);
            // GuiGraphics scissors use screen coordinates, while this canvas has a scaled pose.
            var matrix = g.pose().last().pose();
            var topLeft = matrix.transformPosition(new Vector3f(PLAYER_X, 114, 0));
            var bottomRight = matrix.transformPosition(new Vector3f(PLAYER_X + PLAYER_W, 298, 0));
            g.enableScissor((int) Math.floor(topLeft.x), (int) Math.floor(topLeft.y),
                    (int) Math.ceil(bottomRight.x), (int) Math.ceil(bottomRight.y));
            try {
                // GUI buffer flushes clear their blend state; blit does not enable it itself.
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                g.blit(kit.image(), PLAYER_X + (PLAYER_W - width) / 2, 114 + (184 - height) / 2,
                        width, height, 0, 0, kit.imageWidth(), kit.imageHeight(), kit.imageWidth(), kit.imageHeight());
            } finally { g.disableScissor(); }
        } else {
            // Lightweight neutral fallback until a pack supplies artwork.
            int x = PLAYER_X + PLAYER_W / 2 - 24;
            g.fill(x, 159, x + 48, 207, 0x70505655);
            g.fill(x + 29, 165, x + 32, 190, BLUE);
            g.fill(x + 18, 165, x + 32, 169, BLUE);
            g.fill(x + 20, 189, x + 31, 194, BLUE);
            g.drawCenteredString(ModUtils.getFont(), Component.translatable("cs2lootbox.music.no_artwork"),
                    PLAYER_X + PLAYER_W / 2, 226, MUTED);
        }
        g.fill(PLAYER_X, 296, PLAYER_X + PLAYER_W, 298, 0xFF000000 | kit.grade().color());
    }

    @OnlyIn(Dist.CLIENT) private void drawControls(GuiGraphics g, int mx, int my) {
        Font font = ModUtils.getFont();
        boolean playing = preview != null && preview.hasPlayback();
        boolean paused = preview != null && preview.state() == MusicKitPreview.State.PAUSED;
        button(g, 390, CONTROLS_Y, 34, 30, mx, my, false, !waitingForSave);
        button(g, 430, CONTROLS_Y, 42, 30, mx, my, playing, !waitingForSave);
        button(g, 478, CONTROLS_Y, 34, 30, mx, my, false, playing && preview.canPause() && !waitingForSave);
        button(g, 518, CONTROLS_Y, 34, 30, mx, my, false, !waitingForSave);
        iconPrevious(g, 407, 407);
        if (playing) g.fill(447, 403, 455, 411, TEXT); else triangle(g, 447, 402, true, TEXT);
        if (paused) triangle(g, 492, 402, true, TEXT);
        else { g.fill(491, 402, 494, 412, playing ? TEXT : MUTED); g.fill(497, 402, 500, 412, playing ? TEXT : MUTED); }
        iconNext(g, 535, 407);
        boolean hover = inside(mx, my, 614, CONTROLS_Y, 122, 30);
        g.fill(614, CONTROLS_Y, 736, CONTROLS_Y + 30, waitingForSave ? 0xFF3F4D43 : hover ? 0xFF528F65 : GREEN);
        g.drawCenteredString(font, Component.translatable("cs2lootbox.music.save"), 675, CONTROLS_Y + 11, TEXT);
        String tooltip = null;
        if (inside(mx, my, 390, CONTROLS_Y, 34, 30)) tooltip = "previous";
        else if (inside(mx, my, 430, CONTROLS_Y, 42, 30)) tooltip = playing ? "stop" : "play";
        else if (inside(mx, my, 478, CONTROLS_Y, 34, 30)) tooltip = paused ? "resume" : "pause";
        else if (inside(mx, my, 518, CONTROLS_Y, 34, 30)) tooltip = "next";
        if (tooltip != null) g.drawCenteredString(font, Component.translatable("cs2lootbox.music." + tooltip), 472, 381, BLUE);
    }

    @OnlyIn(Dist.CLIENT) private Component status() {
        return Component.translatable("cs2lootbox.music.state." + (preview == null ? "stopped" : preview.state().name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override @OnlyIn(Dist.CLIENT) public boolean mouseClicked(double mx, double my, int button) {
        if (!ClientMusicKitAvailability.isAvailable(kit)) return false;
        mx -= getPositionX(); my -= getPositionY();
        if (button != 0) return false;
        if (inside(mx, my, 712, 38, 24, 24)) { closeScreen(); return true; }
        if (waitingForSave) return true;
        if (inside(mx, my, BAR_X - 3, LIST_Y, BAR_W + 6, LIST_H) && maxScroll() > 0) {
            draggingScrollbar = true; dragScroll(my); return true;
        }
        if (inside(mx, my, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            int index = scroll + (int) ((my - LIST_Y) / ROW_H);
            if (index < kit.tracks().size()) select(index);
            return true;
        }
        if (inside(mx, my, 390, CONTROLS_Y, 34, 30)) { select((selected + kit.tracks().size() - 1) % kit.tracks().size()); return true; }
        if (inside(mx, my, 518, CONTROLS_Y, 34, 30)) { select((selected + 1) % kit.tracks().size()); return true; }
        if (inside(mx, my, 430, CONTROLS_Y, 42, 30)) { playStop(); return true; }
        if (inside(mx, my, 478, CONTROLS_Y, 34, 30)) { preview().togglePause(); return true; }
        if (inside(mx, my, 614, CONTROLS_Y, 122, 30)) { save(); return true; }
        return inside(mx, my, 40, 24, 720, 432);
    }

    @Override @OnlyIn(Dist.CLIENT) public boolean mouseWheelMove(double mx, double my, double delta) {
        if (!inside(mx - getPositionX(), my - getPositionY(), LIST_X, LIST_Y, LIST_W + 12, LIST_H)) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 2));
        return true;
    }
    @Override @OnlyIn(Dist.CLIENT) public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!draggingScrollbar || button != 0) return false;
        dragScroll(my - getPositionY()); return true;
    }
    @Override @OnlyIn(Dist.CLIENT) public boolean mouseReleased(double mx, double my, int button) {
        boolean handled = draggingScrollbar; draggingScrollbar = false; return handled;
    }
    @Override @OnlyIn(Dist.CLIENT) public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!ClientMusicKitAvailability.isAvailable(kit)) return false;
        if (waitingForSave) return false;
        if (key == GLFW.GLFW_KEY_SPACE) { playStop(); return true; }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            select(Math.max(0, Math.min(kit.tracks().size() - 1, selected + (key == GLFW.GLFW_KEY_UP ? -1 : 1)))); return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT) private void select(int index) {
        selected = index; revealSelection(); error = null;
        preview().play(kit.tracks().get(index));
    }
    
    @OnlyIn(Dist.CLIENT) private void playStop() {
        if (preview().hasPlayback()) preview.close(); else preview.play(kit.tracks().get(selected));
    }
    
    @OnlyIn(Dist.CLIENT) private void save() {
        if (!ClientMusicKitAvailability.isAvailable(kit)) return;
        closePreview(); waitingForSave = true; error = null;
        writeClientAction(SAVE, out -> out.writeVarInt(selected));
    }
    
    @OnlyIn(Dist.CLIENT) private void closeScreen() {
        closePreview();
        ClientUiActions.closeContainer();
    }

    private int maxScroll() { return Math.max(0, kit.tracks().size() - VISIBLE); }
    private int thumbHeight() { return Math.max(24, LIST_H * VISIBLE / kit.tracks().size()); }
    private int thumbY() { return LIST_Y + (maxScroll() == 0 ? 0 : (LIST_H - thumbHeight()) * scroll / maxScroll()); }
    private void revealSelection() {
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + VISIBLE) scroll = selected - VISIBLE + 1;
    }
    private void dragScroll(double y) {
        scroll = Math.max(0, Math.min(maxScroll(), (int) Math.round(
                (y - LIST_Y - thumbHeight() / 2.0) / (LIST_H - thumbHeight()) * maxScroll())));
    }
    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }
    private static String time(double seconds) {
        int value = Math.max(0, (int) seconds);
        return String.format(java.util.Locale.ROOT, "%d:%02d", value / 60, value % 60);
    }
    @OnlyIn(Dist.CLIENT) private static Component fit(Component text, int width, float scale) {
        Font font = ModUtils.getFont();
        String value = text.getString(); int limit = (int) (width / scale);
        return Component.literal(font.width(value) <= limit ? value : font.plainSubstrByWidth(value, Math.max(0, limit - font.width("..."))) + "...");
    }
    @OnlyIn(Dist.CLIENT) private static void label(GuiGraphics g, Component value, int x, int y, float scale, int color) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        g.drawString(ModUtils.getFont(), value, 0, 0, color, false); g.pose().popPose();
    }
    @OnlyIn(Dist.CLIENT) private static void button(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean active, boolean enabled) {
        g.fill(x, y, x + w, y + h, active ? 0xFF486474 : enabled && inside(mx, my, x, y, w, h) ? 0xFF616560 : 0xB534393B);
        g.fill(x, y + h - 1, x + w, y + h, active ? BLUE : 0x50777C79);
    }
    @OnlyIn(Dist.CLIENT) private static void triangle(GuiGraphics g, int x, int y, boolean forward, int color) {
        for (int i = 0; i < 7; i++) {
            int dx = forward ? i : 6 - i;
            g.fill(x + dx, y + i / 2, x + dx + 1, y + 10 - i / 2, color);
        }
    }
    @OnlyIn(Dist.CLIENT) private static void iconPrevious(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 6, cy - 5, cx - 4, cy + 5, TEXT); triangle(g, cx - 3, cy - 5, false, TEXT);
    }
    @OnlyIn(Dist.CLIENT) private static void iconNext(GuiGraphics g, int cx, int cy) {
        triangle(g, cx - 5, cy - 5, true, TEXT); g.fill(cx + 4, cy - 5, cx + 6, cy + 5, TEXT);
    }
}
