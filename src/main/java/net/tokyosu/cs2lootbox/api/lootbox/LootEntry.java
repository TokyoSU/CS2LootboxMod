package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable loot entry for one crate.
 *
 * Weight is expressed as a value from 0 to 100. Entries are normalized against
 * the total enabled weight, so a table totaling 100 behaves like literal
 * percentages while still allowing convenient relative weights.
 */
public record LootEntry(
        @NotNull ResourceLocation itemId,
        boolean itemTagSource,
        double weight,
        int minCount,
        int maxCount,
        @Nullable String nameTranslationKey,
        @Nullable String rarityTranslationKey,
        @Nullable String rarityTier,
        int rarityColor,
        @NotNull List<String> descriptionTranslationKeys,
        @Nullable StatTrackModifier statTrackModifier,
        @Nullable CompoundTag itemNbt,
        @NotNull ItemListTransform itemListTransform) {

    public LootEntry {
        itemId = Objects.requireNonNull(itemId, "itemId");
        descriptionTranslationKeys = List.copyOf(Objects.requireNonNull(descriptionTranslationKeys, "descriptionTranslationKeys"));
        itemNbt = itemNbt == null ? null : itemNbt.copy();
        itemListTransform = Objects.requireNonNull(itemListTransform, "itemListTransform");
    }

    /** Returns a defensive copy because CompoundTag is mutable. */
    @Override
    public @Nullable CompoundTag itemNbt() {
        return itemNbt == null ? null : itemNbt.copy();
    }

    /**
     * Per-entry transform used only by the case item-list card renderer.
     *
     * It intentionally does not affect the authoritative reward ItemStack,
     * carousel/result rendering, or the interactive 3D inspector.
     */
    public record ItemListTransform(
            float offsetX,
            float offsetY,
            float offsetZ,
            float rotationX,
            float rotationY,
            float rotationZ,
            float scaleX,
            float scaleY,
            float scaleZ) {

        public static final ItemListTransform DEFAULT =
                new ItemListTransform(
                        0.0F, 0.0F, 0.0F,
                        0.0F, 0.0F, 0.0F,
                        1.0F, 1.0F, 1.0F
                );
    }
}
