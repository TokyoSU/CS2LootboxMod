package net.tokyosu.cs2lootbox.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Persistent NBT helper for StatTrack rewards. */
public final class StatTrackUtil {
    public static final String ROOT_TAG = "CS2LootboxStatTrack";
    private static final String ENABLED_TAG = "Enabled";
    private static final String KILLS_TAG = "Kills";
    private static final String STARS_TAG = "Stars";
    private static final String VERSION_TAG = "Version";
    private static final int VERSION = 1;

    private StatTrackUtil() {
    }

    public static void initialize(@NotNull ItemStack stack, int stars) {
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag statTrack = new CompoundTag();
        statTrack.putInt(VERSION_TAG, VERSION);
        statTrack.putBoolean(ENABLED_TAG, true);
        statTrack.putInt(KILLS_TAG, 0);
        statTrack.putInt(STARS_TAG, clampStars(stars));
        stack.getOrCreateTag().put(ROOT_TAG, statTrack);

        // StatTrak is part of the item's actual display name, not a separate
        // tooltip line. This works for vanilla and modded items alike because
        // ItemStack serializes the custom hover-name Component into NBT.
        //
        // Preserve whatever name the item already had (including a modded or
        // custom NBT name) and only prefix it once, at mutation time.
        Component originalName = stack.getHoverName().copy();
        stack.setHoverName(createDisplayNamePrefix(stack).append(" ").append(originalName));
    }

    public static boolean isStatTrack(@NotNull ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null
                && root.contains(ROOT_TAG, Tag.TAG_COMPOUND)
                && root.getCompound(ROOT_TAG).getBoolean(ENABLED_TAG);
    }

    public static int getKills(@NotNull ItemStack stack) {
        CompoundTag statTrack = getStatTrackTag(stack);
        return statTrack == null ? 0 : Math.max(0, statTrack.getInt(KILLS_TAG));
    }

    public static int getStars(@NotNull ItemStack stack) {
        CompoundTag statTrack = getStatTrackTag(stack);
        return statTrack == null ? 0 : clampStars(statTrack.getInt(STARS_TAG));
    }

    /**
     * Human-readable mutation stars.
     * 0 -> ""
     * 1 -> "★"
     * 2 -> "★★"
     */
    public static @NotNull String getStarGlyphs(@NotNull ItemStack stack) {
        return "★".repeat(getStars(stack));
    }

    /**
     * Builds the visible StatTrak prefix used by the actual ItemStack name.
     *
     * 0 stars -> StatTrak™
     * 1 star  -> StatTrak™ (★)
     * 2 stars -> StatTrak™ (★★)
     */
    public static @NotNull MutableComponent createDisplayNamePrefix(@NotNull ItemStack stack) {
        String stars = getStarGlyphs(stack);
        return (stars.isEmpty()
                ? Component.translatable("cs2lootbox.stattrack.label_plain")
                : Component.translatable("cs2lootbox.stattrack.label", stars))
                .withStyle(ChatFormatting.GOLD);
    }

    /** Increments and returns the new kill count. */
    public static int incrementKills(@NotNull ItemStack stack) {
        if (!isStatTrack(stack)) {
            return 0;
        }

        CompoundTag root = stack.getOrCreateTag();
        CompoundTag statTrack = root.getCompound(ROOT_TAG);
        int kills = Math.max(0, statTrack.getInt(KILLS_TAG));
        if (kills < Integer.MAX_VALUE) {
            kills++;
        }
        statTrack.putInt(KILLS_TAG, kills);
        root.put(ROOT_TAG, statTrack);
        return kills;
    }

    private static CompoundTag getStatTrackTag(@NotNull ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompound(ROOT_TAG);
    }

    private static int clampStars(int stars) {
        return Math.max(0, Math.min(2, stars));
    }
}
