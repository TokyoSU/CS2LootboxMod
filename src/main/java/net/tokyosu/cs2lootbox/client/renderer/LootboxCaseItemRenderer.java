package net.tokyosu.cs2lootbox.client.renderer;

import net.tokyosu.cs2lootbox.client.model.LootboxCaseItemModel;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer used by the Minecraft case ItemStack.
 *
 * The model uses GeckoLib's normal Minecraft RenderType; there is no custom
 * custom shader or material binding involved.
 */
public final class LootboxCaseItemRenderer extends GeoItemRenderer<LootboxCaseItem> {
    public LootboxCaseItemRenderer() {
        super(new LootboxCaseItemModel());
    }
}
