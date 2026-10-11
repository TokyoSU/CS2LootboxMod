package net.tokyosu.cs2lootbox.item;

import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import software.bernie.geckolib.animatable.GeoItem;

/** Shared rendering contract for static collectibles and model-backed music kits. */
public interface StaticModelledItem extends GeoItem {
    LootboxDefinition getDefinition();
}
