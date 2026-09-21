package net.tokyosu.cs2lootbox.item;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.tokyosu.cs2lootbox.api.lootbox.LegendaryLoot;
import net.tokyosu.cs2lootbox.api.lootbox.LootEntry;
import net.tokyosu.cs2lootbox.api.lootbox.LootRarityGrade;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.client.renderer.LootboxCaseItemRenderer;
import net.tokyosu.cs2lootbox.config.CS2LootboxClientConfig;
import net.tokyosu.cs2lootbox.config.CS2LootboxServerConfig;
import net.tokyosu.cs2lootbox.loot.LootboxLootRoller;
import net.tokyosu.cs2lootbox.ui.LootboxUI;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.Objects;

/**
 * One generic case item class shared by every registered lootbox.
 */
public final class LootboxCaseItem extends Item implements GeoItem, IUIHolder.ItemUI {
    private final LootboxDefinition definition;
    private final RawAnimation itemIdleAnimation;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LootboxCaseItem(@NotNull Properties properties, @NotNull LootboxDefinition definition) {
        super(properties);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.itemIdleAnimation = RawAnimation.begin().thenLoop(definition.animations().itemIdle());

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public @NotNull LootboxDefinition getDefinition() {
        return definition;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return definition.caseTranslationKey();
    }

    @Override
    public @NotNull String getDescriptionId(@NotNull ItemStack stack) {
        return definition.caseTranslationKey();
    }

    /**
     * Builds the case contents tooltip directly from the immutable loot table
     * that was finalized during the KubeJS startup registration event.
     *
     * This intentionally does not show numeric chances, matching CS2's case
     * tooltip: it shows the possible normal rewards in declaration order and a
     * single gold "rare special item" line when the case has legendary loot.
     */
    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Level level,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (!CS2LootboxClientConfig.SHOW_CASE_LOOT_TOOLTIP.get()) {
            return;
        }

        String collection = definition.resultCollectionTranslationKey();
        if (collection != null && !collection.isBlank()) {
            tooltip.add(configuredText(collection)
                    .copy()
                    .withStyle(ChatFormatting.GRAY));
        }

        if (definition.loot().isEmpty() && definition.legendaryLoot().isEmpty()) {
            return;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("cs2lootbox.tooltip.contains")
                .withStyle(ChatFormatting.GRAY));

        for (LootEntry entry : definition.loot()) {
            if (entry == null || !Double.isFinite(entry.weight()) || entry.weight() <= 0.0D) {
                continue;
            }

            ItemStack preview = createTooltipStack(entry);
            Component name = entry.nameTranslationKey() != null
                    ? Component.translatable(entry.nameTranslationKey())
                    : preview.getHoverName();

            tooltip.add(name.copy().setStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(resolveRarityColor(entry, preview)))
            ));
        }

        for (LegendaryLoot legendary : definition.legendaryLoot()) {
            if (legendary == null
                    || !Double.isFinite(legendary.weight())
                    || legendary.weight() <= 0.0D
                    || legendary.subLoot().isEmpty()) {
                continue;
            }

            String legendaryTooltip = legendary.tooltipText();
            Component line = legendaryTooltip == null || legendaryTooltip.isBlank()
                    ? Component.translatable("cs2lootbox.tooltip.rare_special")
                    : configuredText(legendaryTooltip);

            // One line per legendary() panel, in declaration order. This lets
            // one case advertise knives, gloves, or any number of custom
            // legendary categories independently.
            tooltip.add(line.copy().withStyle(ChatFormatting.GOLD));
        }
    }

    private static @NotNull ItemStack createTooltipStack(@NotNull LootEntry entry) {
        return LootboxLootRoller.createPreviewStack(entry, 1);
    }

    private static @NotNull Component configuredText(@NotNull String value) {
        String trimmed = value.trim();

        // collectionText(...) accepts both a literal and a translation key.
        // Translation keys conventionally contain no spaces; if the key is
        // missing Minecraft simply displays the key itself, which is still a
        // useful fallback.
        // Translation keys conventionally contain dots (namespace-style keys
        // such as tooltip.kubejs.rare_gloves). A one-word literal like
        // "Knives" should stay literal instead of being treated as a key.
        if (trimmed.indexOf('.') < 0) {
            return Component.literal(trimmed);
        }

        return Component.translatable(trimmed);
    }

    private static int resolveRarityColor(@NotNull LootEntry entry, @NotNull ItemStack stack) {
        if (entry.rarityColor() != -1) {
            return entry.rarityColor() & 0xFFFFFF;
        }

        String tier = entry.rarityTier();
        if (tier != null && !tier.isBlank()) {
            try {
                return LootRarityGrade.fromId(tier.toLowerCase(Locale.ROOT)).color();
            } catch (IllegalArgumentException ignored) {
                // Custom rarity tier: fall through to the vanilla item rarity.
            }
        }

        if (!stack.isEmpty()) {
            return switch (stack.getRarity()) {
                case UNCOMMON -> 0xFFFF55;
                case RARE -> 0x55FFFF;
                case EPIC -> 0xFF55FF;
                default -> 0xFFFFFF;
            };
        }

        return 0xFFFFFF;
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private LootboxCaseItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new LootboxCaseItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(@NotNull AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "item",
                0,
                state -> state.setAndContinue(itemIdleAnimation)
        ));
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer serverPlayer) {
            if (!CS2LootboxServerConfig.ALLOW_CASE_OPENING.get()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("cs2lootbox.message.opening_disabled"),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, hand);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull ModularUI createUI(@NotNull Player entityPlayer, @NotNull HeldItemUIFactory.HeldItemHolder holder) {
        return LootboxUI.create(entityPlayer, holder, definition);
    }
}
