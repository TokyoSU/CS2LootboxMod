package net.tokyosu.cs2lootbox.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.api.lootbox.ModelItemDefinitionBuilder;
import net.tokyosu.cs2lootbox.client.renderer.ModelItemRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

/** A static collectible; it has no lootbox UI or animation controller. */
public final class ModelItem extends Item implements GeoItem {
    private final LootboxDefinition definition;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public ModelItem(Properties properties, ModelItemDefinitionBuilder.Definition definition) {
        super(properties.rarity(definition.grade().minecraftRarity()));
        this.definition = definition.display();
    }
    public LootboxDefinition getDefinition() { return definition; }
    @Override public String getDescriptionId() { return definition.caseTranslationKey(); }
    @Override public String getDescriptionId(ItemStack stack) { return getDescriptionId(); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ModelItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ModelItemRenderer();
                return renderer;
            }
        });
    }
}
