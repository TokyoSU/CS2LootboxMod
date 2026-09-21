package net.tokyosu.cs2lootbox.ui;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.world.entity.player.Player;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.client.widget.LootboxModelWidget;
import net.tokyosu.cs2lootbox.client.widget.LootboxOverlayWidget;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime lootbox opening UI.
 *
 * The UI is authored at a fixed 960x540 reference resolution. ScaledWidgetGroup
 * behaves like Unity's Canvas Scaler / Scale With Screen Size: it uniformly
 * fits the reference canvas into Minecraft's current logical GUI dimensions and
 * remaps mouse input into the same reference coordinate system.
 */
public final class LootboxUI {
    public static final int REFERENCE_WIDTH = 960;
    public static final int REFERENCE_HEIGHT = 540;

    private static final int CONTENT_X = 0;
    private static final int CONTENT_Y = 0;
    private static final int CONTENT_WIDTH = REFERENCE_WIDTH;
    private static final int CONTENT_HEIGHT = REFERENCE_HEIGHT;

    private LootboxUI() {
    }

    public static @NotNull ModularUI create(
            @NotNull Player player,
            @NotNull HeldItemUIFactory.HeldItemHolder holder,
            @NotNull LootboxDefinition definition) {
        // Keep ModularUI itself in the same reference coordinate system. The
        // ScaledWidgetGroup performs the screen fitting at render/input time.
        WidgetGroup root = new WidgetGroup(0, 0, REFERENCE_WIDTH, REFERENCE_HEIGHT);

        ScaledWidgetGroup canvas = new ScaledWidgetGroup(REFERENCE_WIDTH, REFERENCE_HEIGHT);
        canvas.setId("scaled_canvas");
        canvas.setBackground(new ColorRectTexture(0x00000000));
        root.addWidget(canvas);

        LootboxModelWidget caseModel = new LootboxModelWidget(
                CONTENT_X,
                CONTENT_Y,
                CONTENT_WIDTH,
                CONTENT_HEIGHT
        );
        caseModel.setId("case_model");
        caseModel.setLootboxId(definition.id());
        caseModel.resetAnimation();
        canvas.addWidget(caseModel);

        // Kept as a separate sibling so GeckoLib's 3D pass cannot overwrite
        // the normal GUI state used by the CS2 header/footer controls.
        LootboxOverlayWidget overlay = new LootboxOverlayWidget(
                CONTENT_X,
                CONTENT_Y,
                CONTENT_WIDTH,
                CONTENT_HEIGHT,
                caseModel
        );
        overlay.setId("case_overlay");
        canvas.addWidget(overlay);

        ModularUI ui = new ModularUI(root, holder, player);
        ui.registerCloseListener(() -> caseModel.handleUiClosed(player));
        return ui;
    }
}
