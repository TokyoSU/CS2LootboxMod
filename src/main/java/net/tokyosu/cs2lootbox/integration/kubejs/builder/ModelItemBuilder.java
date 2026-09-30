package net.tokyosu.cs2lootbox.integration.kubejs.builder;

import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.client.LangEventJS;
import net.minecraft.world.item.Item;
import net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder;
import net.tokyosu.cs2lootbox.item.ModelItem;

public final class ModelItemBuilder extends ItemBuilder {
    private final ModelItemDefinitionBuilder.Definition definition;
    public ModelItemBuilder(ModelItemDefinitionBuilder.Definition definition) {
        super(definition.display().caseItemId());
        this.definition = definition;
        maxStackSize(definition.display().caseStackSize());
        parentModel(definition.display().itemJson().toString());
        translationKey(definition.display().caseTranslationKey());
    }
    @Override public Item createObject() { return new ModelItem(createItemProperties(), definition); }
    @Override public void generateLang(LangEventJS lang) {}
}
