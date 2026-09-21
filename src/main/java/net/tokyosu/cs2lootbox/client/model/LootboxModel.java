package net.tokyosu.cs2lootbox.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.client.animation.LootboxAnimatable;
import software.bernie.geckolib.model.GeoModel;
import org.jetbrains.annotations.NotNull;

/**
 * Generic GUI model. Resource paths come from the selected lootbox definition.
 */
public final class LootboxModel extends GeoModel<LootboxAnimatable> {
    @Override
    public @NotNull ResourceLocation getModelResource(@NotNull LootboxAnimatable animatable) {
        return animatable.getDefinition().model();
    }

    @Override
    public @NotNull ResourceLocation getTextureResource(@NotNull LootboxAnimatable animatable) {
        return animatable.getDefinition().texture();
    }

    @Override
    public @NotNull ResourceLocation getAnimationResource(@NotNull LootboxAnimatable animatable) {
        return animatable.getDefinition().animation();
    }
}
