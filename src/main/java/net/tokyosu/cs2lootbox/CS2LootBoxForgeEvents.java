package net.tokyosu.cs2lootbox;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tokyosu.cs2lootbox.config.CS2LootboxClientConfig;
import net.tokyosu.cs2lootbox.config.CS2LootboxServerConfig;
import net.tokyosu.cs2lootbox.loot.StatTrackUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Forge gameplay/client events used by CS2 Lootbox.
 *
 * The runtime UI is authored entirely in Java with LDLib widgets; the old
 * LDLib in-game UI editor command has been removed.
 */
@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID)
public final class CS2LootBoxForgeEvents {
    private CS2LootBoxForgeEvents() {
    }


    /**
     * Counts any living-entity kill caused by a player while a StatTrack item is
     * held. Main hand is preferred; offhand is used when the main-hand item is
     * not StatTrack-enabled. The NBT mutation occurs only on the logical server.
     */
    @SubscribeEvent
    public static void onLivingDeath(@NotNull LivingDeathEvent event) {
        if (!CS2LootboxServerConfig.ENABLE_STATTRACK_KILL_COUNTING.get()) {
            return;
        }
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        if (event.getEntity() == killer) {
            return;
        }

        ItemStack trackingStack = killer.getMainHandItem();
        if (!StatTrackUtil.isStatTrack(trackingStack)) {
            trackingStack = killer.getOffhandItem();
        }
        if (!StatTrackUtil.isStatTrack(trackingStack)) {
            return;
        }

        StatTrackUtil.incrementKills(trackingStack);
        killer.getInventory().setChanged();
    }

    /** Adds only the current StatTrack kill count below the item's normal display-name line. */
    @SubscribeEvent
    public static void onItemTooltip(@NotNull ItemTooltipEvent event) {
        if (!CS2LootboxClientConfig.SHOW_STATTRACK_KILL_COUNT.get()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!StatTrackUtil.isStatTrack(stack)) {
            return;
        }

        // Do not add a second "StatTrak" tooltip row. The actual ItemStack
        // display name already carries the StatTrak prefix, so Minecraft's
        // normal first tooltip line shows it automatically.
        event.getToolTip().add(Component.translatable("cs2lootbox.stattrack.kills", StatTrackUtil.getKills(stack))
                .withStyle(ChatFormatting.GRAY));
    }

}
