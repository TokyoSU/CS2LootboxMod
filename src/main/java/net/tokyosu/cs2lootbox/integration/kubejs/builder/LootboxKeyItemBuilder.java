package net.tokyosu.cs2lootbox.integration.kubejs.builder;

import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.world.item.Item;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.item.LootboxKeyItem;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * KubeJS registry builder for the simple 2D key item paired with a crate.
 */
public final class LootboxKeyItemBuilder extends ItemBuilder {
    private final LootboxDefinition definition;

    public LootboxKeyItemBuilder(@NotNull LootboxDefinition definition) {
        super(definition.keyItemId());
        this.definition = Objects.requireNonNull(definition, "definition");

        maxStackSize(definition.keyStackSize());
        texture(definition.keyTexture().toString());
        translationKey(definition.keyTranslationKey());
    }

    @Override
    public @NotNull Item createObject() {
        return new LootboxKeyItem(createItemProperties(), definition);
    }

    @Override
    public void generateLang(@NotNull LangEventJS lang) {
        // Names are real translation keys supplied by the resource pack / KubeJS assets.
        // Do not auto-generate an English literal here.
    }
}
