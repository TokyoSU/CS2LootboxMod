package net.tokyosu.cs2lootbox.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * Generic key item paired with a {@link LootboxDefinition}.
 */
public final class LootboxKeyItem extends Item {
    private final LootboxDefinition definition;

    public LootboxKeyItem(@NotNull Properties properties, @NotNull LootboxDefinition definition) {
        super(properties);
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public @NotNull LootboxDefinition getDefinition() {
        return definition;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return definition.keyTranslationKey();
    }

    @Override
    public @NotNull String getDescriptionId(@NotNull ItemStack stack) {
        return definition.keyTranslationKey();
    }
}
