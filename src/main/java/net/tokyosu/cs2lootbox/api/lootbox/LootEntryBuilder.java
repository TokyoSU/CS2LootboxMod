package net.tokyosu.cs2lootbox.api.lootbox;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** KubeJS-friendly builder for a single weighted loot entry. */
@SuppressWarnings("unused")
public final class LootEntryBuilder {
    private final ResourceLocation itemId;
    private final boolean itemTagSource;
    private final double weight;

    private int minCount = 1;
    private int maxCount = 1;
    private String nameTranslationKey;
    private String rarityTranslationKey;
    private String rarityTier;
    private int rarityColor = -1;
    private final List<String> descriptionTranslationKeys = new ArrayList<>();
    private CompoundTag itemNbt;

    // Per-item transform for the small item_list card only.
    private float itemListOffsetX = 0.0F;
    private float itemListOffsetY = 0.0F;
    private float itemListOffsetZ = 0.0F;
    private float itemListRotationX = 0.0F;
    private float itemListRotationY = 0.0F;
    private float itemListRotationZ = 0.0F;
    private float itemListScaleX = 1.0F;
    private float itemListScaleY = 1.0F;
    private float itemListScaleZ = 1.0F;

    /** Modifier collection exposed directly to KubeJS as loot.modifiers. */
    public final LootModifiersBuilder modifiers = new LootModifiersBuilder();

    /** JavaBean accessor so KubeJS/Rhino can resolve loot.modifiers reliably. */
    public @NotNull LootModifiersBuilder getModifiers() {
        return modifiers;
    }

    LootEntryBuilder(@NotNull ResourceLocation itemId, double weight) {
        this(itemId, weight, false);
    }

    LootEntryBuilder(@NotNull ResourceLocation itemId, double weight, boolean itemTagSource) {
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.itemTagSource = itemTagSource;

        if (!Double.isFinite(weight) || weight < 0.0D || weight > 100.0D) {
            throw new IllegalArgumentException("Loot weight must be between 0 and 100: " + weight);
        }
        this.weight = weight;
    }

    @Info("Sets a fixed stack count for this reward.")
    public @NotNull LootEntryBuilder count(int count) {
        int clamped = clampCount(count);
        minCount = clamped;
        maxCount = clamped;
        return this;
    }

    @Info("Sets a random inclusive stack-count range for this reward.")
    public @NotNull LootEntryBuilder count(int min, int max) {
        int a = clampCount(min);
        int b = clampCount(max);
        minCount = Math.min(a, b);
        maxCount = Math.max(a, b);
        return this;
    }

    /** Alias matching crate terminology. */
    public @NotNull LootEntryBuilder stackSize(int count) {
        return count(count);
    }

    /** Alias matching crate terminology. */
    public @NotNull LootEntryBuilder stackSize(int min, int max) {
        return count(min, max);
    }

    @Info("Overrides the prize name with a Minecraft translation key. If omitted, the ItemStack hover name is used.")
    public @NotNull LootEntryBuilder name(@NotNull String translationKey) {
        nameTranslationKey = requireTranslationKey(translationKey, "name");
        return this;
    }

    @Info("Sets the translated rarity label shown on the final prize screen.")
    public @NotNull LootEntryBuilder rarity(@NotNull String translationKey) {
        rarityTranslationKey = requireTranslationKey(translationKey, "rarity");
        if (rarityTier == null) {
            rarityTier = inferTierFromKey(rarityTranslationKey);
        }
        return this;
    }

    @Info("Sets the translated rarity label and RGB/ARGB display color shown on the final prize screen.")
    public @NotNull LootEntryBuilder rarity(@NotNull String translationKey, int color) {
        rarityTranslationKey = requireTranslationKey(translationKey, "rarity");
        if (rarityTier == null) {
            rarityTier = inferTierFromKey(rarityTranslationKey);
        }
        rarityColor = color;
        return this;
    }

    @Info("Sets the RGB/ARGB rarity color while keeping the default rarity label.")
    public @NotNull LootEntryBuilder rarityColor(int color) {
        rarityColor = color;
        return this;
    }

    @Info("Sets the internal rarity tier used by the CS2 UI. Supported values include consumer, industrial, milspec, restricted, classified, covert and special.")
    public @NotNull LootEntryBuilder rarityTier(@NotNull String tier) {
        if (tier == null || tier.isBlank()) {
            throw new IllegalArgumentException("Loot rarity tier cannot be empty");
        }
        rarityTier = tier.trim().toLowerCase(Locale.ROOT);
        return this;
    }

    @Info("Adds one translated description line to the final prize screen.")
    public @NotNull LootEntryBuilder description(@NotNull String translationKey) {
        descriptionTranslationKeys.add(requireTranslationKey(translationKey, "description"));
        return this;
    }

    @Info("Sets the ItemStack NBT from SNBT text. This NBT is applied to item_list, carousel, inspector and the final server reward.")
    public @NotNull LootEntryBuilder nbt(@NotNull String snbt) {
        if (snbt == null || snbt.isBlank()) {
            itemNbt = null;
            return this;
        }

        try {
            itemNbt = TagParser.parseTag(snbt.trim());
        } catch (CommandSyntaxException exception) {
            throw new IllegalArgumentException("Invalid loot ItemStack SNBT: " + snbt, exception);
        }
        return this;
    }

    @Info("Sets the ItemStack NBT from a CompoundTag. A defensive copy is stored.")
    public @NotNull LootEntryBuilder nbt(@NotNull CompoundTag nbt) {
        itemNbt = Objects.requireNonNull(nbt, "nbt").copy();
        return this;
    }

    @Info("Alias for nbt(...). In Minecraft 1.20.1 the ItemStack NBT root is also commonly called the stack tag.")
    public @NotNull LootEntryBuilder tag(@NotNull String snbt) {
        return nbt(snbt);
    }

    @Info("Alias for nbt(...). A defensive copy of the CompoundTag is stored.")
    public @NotNull LootEntryBuilder tag(@NotNull CompoundTag nbt) {
        return nbt(nbt);
    }

    @Info("Removes any configured ItemStack NBT/tag from this loot entry.")
    public @NotNull LootEntryBuilder clearNbt() {
        itemNbt = null;
        return this;
    }

    @Info("Sets the X/Y offset, in GUI pixels, for this item. Only affects the case item_list.")
    public @NotNull LootEntryBuilder position(float x, float y) {
        return position(x, y, itemListOffsetZ);
    }

    @Info("Sets the X/Y GUI offset and Z render offset for this item. Only affects the case item_list.")
    public @NotNull LootEntryBuilder position(float x, float y, float z) {
        itemListOffsetX = finite(x, "item-list position X");
        itemListOffsetY = finite(y, "item-list position Y");
        itemListOffsetZ = finite(z, "item-list position Z");
        return this;
    }

    /** Short alias for position(...). Only affects the case item_list. */
    public @NotNull LootEntryBuilder pos(float x, float y) {
        return position(x, y);
    }

    /** Short alias for position(...). Only affects the case item_list. */
    public @NotNull LootEntryBuilder pos(float x, float y, float z) {
        return position(x, y, z);
    }

    @Info("Sets this item's rotation in degrees around X, Y and Z. Only affects the case item_list.")
    public @NotNull LootEntryBuilder rotation(float x, float y, float z) {
        itemListRotationX = finite(x, "item-list rotation X");
        itemListRotationY = finite(y, "item-list rotation Y");
        itemListRotationZ = finite(z, "item-list rotation Z");
        return this;
    }

    /** Short alias for rotation(...). Only affects the case item_list. */
    public @NotNull LootEntryBuilder rot(float x, float y, float z) {
        return rotation(x, y, z);
    }

    @Info("Sets a uniform scale for this item. 1.0 is the default. Only affects the case item_list.")
    public @NotNull LootEntryBuilder scale(float scale) {
        float value = positiveFinite(scale, "item-list scale");
        itemListScaleX = value;
        itemListScaleY = value;
        itemListScaleZ = value;
        return this;
    }

    @Info("Sets independent X/Y/Z scale values. 1.0, 1.0, 1.0 is the default. Only affects the case item_list.")
    public @NotNull LootEntryBuilder scale(float x, float y, float z) {
        itemListScaleX = positiveFinite(x, "item-list scale X");
        itemListScaleY = positiveFinite(y, "item-list scale Y");
        itemListScaleZ = positiveFinite(z, "item-list scale Z");
        return this;
    }

    @Info("Sets position, rotation and a uniform scale in one call. Only affects the case item_list.")
    public @NotNull LootEntryBuilder transform(
            float x,
            float y,
            float z,
            float rotationX,
            float rotationY,
            float rotationZ,
            float scale) {
        return position(x, y, z)
                .rotation(rotationX, rotationY, rotationZ)
                .scale(scale);
    }

    @NotNull LootEntry build() {
        return new LootEntry(
                itemId,
                itemTagSource,
                weight,
                minCount,
                maxCount,
                nameTranslationKey,
                rarityTranslationKey,
                rarityTier,
                rarityColor,
                descriptionTranslationKeys,
                modifiers.statTrack(),
                itemNbt,
                new LootEntry.ItemListTransform(
                        itemListOffsetX,
                        itemListOffsetY,
                        itemListOffsetZ,
                        itemListRotationX,
                        itemListRotationY,
                        itemListRotationZ,
                        itemListScaleX,
                        itemListScaleY,
                        itemListScaleZ
                )
        );
    }

    private static float finite(float value, @NotNull String field) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Loot " + field + " must be finite: " + value);
        }
        return value;
    }

    private static float positiveFinite(float value, @NotNull String field) {
        value = finite(value, field);
        if (value <= 0.0F) {
            throw new IllegalArgumentException("Loot " + field + " must be greater than 0: " + value);
        }
        return value;
    }

    private static int clampCount(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Loot stack count must be at least 1: " + value);
        }
        return Math.min(64, value);
    }

    private static @NotNull String requireTranslationKey(@NotNull String value, @NotNull String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Loot " + field + " translation key cannot be empty");
        }
        return value.trim();
    }

    private static @Nullable String inferTierFromKey(@NotNull String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.contains("special") || lower.contains("gold") || lower.contains("knife") || lower.contains("glove")) {
            return "special";
        }
        if (lower.contains("covert") || lower.contains("red")) {
            return "covert";
        }
        if (lower.contains("classified") || lower.contains("pink")) {
            return "classified";
        }
        if (lower.contains("restricted") || lower.contains("purple")) {
            return "restricted";
        }
        if (lower.contains("milspec") || lower.contains("mil_spec") || lower.contains("blue")) {
            return "milspec";
        }
        if (lower.contains("industrial") || lower.contains("light_blue")) {
            return "industrial";
        }
        if (lower.contains("consumer") || lower.contains("white") || lower.contains("gray")) {
            return "consumer";
        }
        return null;
    }
}
