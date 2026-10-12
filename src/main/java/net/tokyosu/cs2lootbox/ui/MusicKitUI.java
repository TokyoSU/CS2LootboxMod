package net.tokyosu.cs2lootbox.ui;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.world.entity.player.Player;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.client.widget.MusicKitWidget;

/** Reuses the case screen's responsive canvas and held-item networking, without any carousel. */
public final class MusicKitUI {
    public static final int WIDTH = 800, HEIGHT = 480;
    
    private MusicKitUI() { }
    
    @SuppressWarnings("resource")
    public static ModularUI create(Player player, HeldItemUIFactory.HeldItemHolder holder, MusicKitDefinition kit, int selected) {
        WidgetGroup root = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        ScaledWidgetGroup canvas = new ScaledWidgetGroup(WIDTH, HEIGHT);
        root.addWidget(canvas);
        MusicKitWidget music = new MusicKitWidget(kit, selected);
        music.setId("music_kit");
        canvas.addWidget(music);
        ModularUI ui = new ModularUI(root, holder, player);
        ui.registerCloseListener(() -> { if (player.level().isClientSide) music.closePreview(); });
        return ui;
    }
}
