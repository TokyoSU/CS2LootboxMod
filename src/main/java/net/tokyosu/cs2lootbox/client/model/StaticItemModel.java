package net.tokyosu.cs2lootbox.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.item.ModelItem;
import software.bernie.geckolib.model.GeoModel;
import org.jetbrains.annotations.NotNull;

/**
 * Generic GeckoLib ItemStack model backed by the case's lootbox definition.
 */
public final class StaticItemModel extends GeoModel<ModelItem> {
    @Override
    public @NotNull ResourceLocation getModelResource(@NotNull ModelItem animatable) {
        return animatable.getDefinition().model();
    }

    @Override
    public @NotNull ResourceLocation getTextureResource(@NotNull ModelItem animatable) {
        return animatable.getDefinition().texture();
    }

    @Override
    public @NotNull ResourceLocation getAnimationResource(@NotNull ModelItem animatable) {
        return animatable.getDefinition().animation();
    }
}

