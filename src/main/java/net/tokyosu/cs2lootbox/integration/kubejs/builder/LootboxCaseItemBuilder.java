package net.tokyosu.cs2lootbox.integration.kubejs.builder;

import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.world.item.Item;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * KubeJS registry builder for a GeckoLib-backed lootbox case item.
 */
public final class LootboxCaseItemBuilder extends ItemBuilder {
    private final LootboxDefinition definition;

    public LootboxCaseItemBuilder(@NotNull LootboxDefinition definition) {
        super(definition.caseItemId());
        this.definition = Objects.requireNonNull(definition, "definition");

        maxStackSize(definition.caseStackSize());

        // Let KubeJS generate this case's model as a child of the selected
        // shared item JSON. The shared JSON only controls vanilla item display
        // transforms; LootboxCaseItem's custom GeckoLib renderer still draws
        // the actual .geo.json model.
        parentModel(definition.itemJson().toString());

        translationKey(definition.caseTranslationKey());
    }

    @Override
    public @NotNull Item createObject() {
        return new LootboxCaseItem(createItemProperties(), definition);
    }

    @Override
    public void generateLang(@NotNull LangEventJS lang) {
        // Names are real translation keys supplied by the resource pack / KubeJS assets.
        // Do not auto-generate an English literal here.
    }

}
