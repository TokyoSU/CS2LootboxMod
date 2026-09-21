package net.tokyosu.cs2lootbox.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import software.bernie.geckolib.model.GeoModel;
import org.jetbrains.annotations.NotNull;

/**
 * Generic GeckoLib ItemStack model backed by the case's lootbox definition.
 */
public final class LootboxCaseItemModel extends GeoModel<LootboxCaseItem> {
    @Override
    public @NotNull ResourceLocation getModelResource(@NotNull LootboxCaseItem animatable) {
        return animatable.getDefinition().model();
    }

    @Override
    public @NotNull ResourceLocation getTextureResource(@NotNull LootboxCaseItem animatable) {
        return animatable.getDefinition().texture();
    }

    @Override
    public @NotNull ResourceLocation getAnimationResource(@NotNull LootboxCaseItem animatable) {
        return animatable.getDefinition().animation();
    }
}
