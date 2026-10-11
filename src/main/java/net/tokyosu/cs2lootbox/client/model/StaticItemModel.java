package net.tokyosu.cs2lootbox.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.tokyosu.cs2lootbox.item.StaticModelledItem;
import software.bernie.geckolib.model.GeoModel;
import org.jetbrains.annotations.NotNull;

/**
 * Generic GeckoLib ItemStack model backed by the case's lootbox definition.
 */
public final class StaticItemModel<T extends Item & StaticModelledItem> extends GeoModel<T> {
    @Override
    public @NotNull ResourceLocation getModelResource(@NotNull T animatable) {
        return animatable.getDefinition().model();
    }

    @Override
    public @NotNull ResourceLocation getTextureResource(@NotNull T animatable) {
        return animatable.getDefinition().texture();
    }

    @Override
    public @NotNull ResourceLocation getAnimationResource(@NotNull T animatable) {
        return animatable.getDefinition().animation();
    }
}

