package net.tokyosu.cs2lootbox.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Keep LocalPlayer references and client lambdas out of server-instantiated widgets. */
@OnlyIn(Dist.CLIENT)
public final class ClientUiActions {
    private ClientUiActions() { }

    public static void closeContainer() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.closeContainer();
    }

    public static void showActionBar(Component message) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.displayClientMessage(message, true);
    }

    /** Wait for the server's close-container packet before opening the inventory. */
    public static void openInventoryNextTick() {
        var minecraft = Minecraft.getInstance();
        minecraft.tell(() -> {
            if (minecraft.player != null) minecraft.setScreen(new InventoryScreen(minecraft.player));
        });
    }
}
