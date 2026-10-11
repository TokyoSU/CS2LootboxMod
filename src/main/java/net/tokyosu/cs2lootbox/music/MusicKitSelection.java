package net.tokyosu.cs2lootbox.music;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.item.MusicKitItem;

/** Only a stable track ID is saved. Playback position is never serialized or accepted from the client. */
public final class MusicKitSelection {
    private static final String TRACK = "CS2MusicTrack", ORIGINAL = "CS2MusicKitOriginal";
    private MusicKitSelection() { }

    public static int selectedTrack(ItemStack stack, MusicKitDefinition kit) {
        if (stack.getItem() instanceof MusicKitItem item && !item.isBase()) return item.trackIndex();
        String id = stack.hasTag() ? stack.getTag().getString(TRACK) : "";
        for (int i = 0; i < kit.tracks().size(); i++) if (kit.tracks().get(i).id().equals(id)) return i;
        return 0;
    }

    public static ItemStack savedCopy(ItemStack held, MusicKitDefinition kit, int index) {
        if (index < 0 || index >= kit.tracks().size() || held.getCount() != 1
                || !(held.getItem() instanceof MusicKitItem item) || item.definition() != kit) return ItemStack.EMPTY;
        ItemStack result = item.isBase() ? held.copy() : restoreKit(held);
        if (!result.isEmpty()) result.getOrCreateTag().putString(TRACK, kit.tracks().get(index).id());
        return result;
    }

    public static ItemStack jukeboxDisc(ItemStack held, MusicKitDefinition kit) {
        if (held.isEmpty() || !(held.getItem() instanceof MusicKitItem item) || !item.isBase() || item.definition() != kit)
            return ItemStack.EMPTY;
        int index = selectedTrack(held, kit);
        var disc = ForgeRegistries.ITEMS.getValue(kit.itemId(index));
        if (!(disc instanceof MusicKitItem music) || music.isBase() || music.definition() != kit) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(disc);
        result.getOrCreateTag().put(ORIGINAL, held.copyWithCount(1).save(new CompoundTag()));
        return result;
    }

    /** Vanilla jukebox ejection becomes the same modelled kit with its custom name / NBT intact. */
    public static ItemStack restoreKit(ItemStack disc) {
        if (!(disc.getItem() instanceof MusicKitItem item) || item.isBase()) return disc;
        MusicKitDefinition kit = item.definition();
        ItemStack result = disc.hasTag() ? ItemStack.of(disc.getTag().getCompound(ORIGINAL)) : ItemStack.EMPTY;
        if (!(result.getItem() instanceof MusicKitItem original) || !original.isBase() || original.definition() != kit) {
            var base = ForgeRegistries.ITEMS.getValue(kit.id());
            if (!(base instanceof MusicKitItem)) return ItemStack.EMPTY;
            result = new ItemStack(base);
        }
        result.setCount(1);
        result.getOrCreateTag().putString(TRACK, kit.tracks().get(item.trackIndex()).id());
        return result;
    }
}
