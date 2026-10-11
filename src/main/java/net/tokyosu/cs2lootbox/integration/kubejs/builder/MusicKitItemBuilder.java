package net.tokyosu.cs2lootbox.integration.kubejs.builder;

import dev.latvian.mods.kubejs.client.LangEventJS;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.item.MusicKitItem;

public final class MusicKitItemBuilder extends ItemBuilder {
    private final MusicKitDefinition definition;
    private final int track;

    public MusicKitItemBuilder(ResourceLocation id, MusicKitDefinition definition, int track) {
        super(id);
        this.definition = definition; this.track = track;
        maxStackSize(1);
        if (id.equals(definition.id()) && definition.display() != null) parentModel(definition.display().itemJson().toString());
        else texture(id.equals(definition.id()) ? definition.itemTexture().toString() : "minecraft:item/music_disc_13");
        tag(ResourceLocation.fromNamespaceAndPath("minecraft", "music_discs"));
    }

    @Override public Item createObject() { return new MusicKitItem(createItemProperties(), definition, track, id.equals(definition.id())); }
    @Override public void generateLang(LangEventJS lang) { }
}
