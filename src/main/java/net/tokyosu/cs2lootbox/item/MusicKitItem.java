package net.tokyosu.cs2lootbox.item;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.stats.Stats;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.tokyosu.cs2lootbox.client.renderer.ModelItemRenderer;
import net.tokyosu.cs2lootbox.client.music.ClientMusicKitAvailability;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.music.MusicKitSelection;
import net.tokyosu.cs2lootbox.ui.MusicKitUI;
import java.util.List;
import java.util.function.Consumer;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A normal vanilla-compatible music disc that can also open its complete kit library. */
public final class MusicKitItem extends RecordItem implements IUIHolder.ItemUI, StaticModelledItem {
    private final MusicKitDefinition definition;
    private final int track;
    private final boolean base;
    private AnimatableInstanceCache cache;

    public MusicKitItem(Properties properties, MusicKitDefinition definition, int track, boolean base) {
        super(1, () -> SoundEvent.createVariableRangeEvent(definition.tracks().get(track).sound()),
                properties.stacksTo(1).rarity(definition.grade().minecraftRarity()),
                definition.tracks().get(track).lengthInTicks());
        this.definition = definition; this.track = track; this.base = base;
    }

    public MusicKitDefinition definition() { return definition; }
    public int trackIndex() { return track; }
    public boolean isBase() { return base; }
    @Override public Component getName(ItemStack stack) { return definition.nameComponent(); }
    @Override public MutableComponent getDisplayName() { return definition.tracks().get(track).titleComponent().copy(); }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(definition.tracks().get(MusicKitSelection.selectedTrack(stack, definition)).titleComponent().copy()
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("cs2lootbox.music.tooltip").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && !ClientMusicKitAvailability.isAvailable(definition)) {
            player.displayClientMessage(Component.translatable("cs2lootbox.music.missing_resources"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer server) HeldItemUIFactory.INSTANCE.openUI(server, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override public ModularUI createUI(Player player, HeldItemUIFactory.HeldItemHolder holder) {
        return MusicKitUI.create(player, holder, definition, MusicKitSelection.selectedTrack(holder.getHeld(), definition));
    }

    @Override public LootboxDefinition getDefinition() {
        if (definition.display() == null || !base) throw new IllegalStateException("This disc has no 3D model");
        return definition.display();
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        if (cache == null) cache = GeckoLibUtil.createInstanceCache(this);
        return cache;
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Forge calls this from Item's constructor, before our definition/base fields are assigned.
        consumer.accept(new IClientItemExtensions() {
            private ModelItemRenderer<MusicKitItem> renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (!base || definition.display() == null) return IClientItemExtensions.super.getCustomRenderer();
                if (renderer == null) renderer = new ModelItemRenderer<>();
                return renderer;
            }
        });
    }

    /** Adapt only the inserted stack to a hidden fixed-track vanilla disc; the held kit keeps its model. */
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!base) return super.useOn(context);
        Level level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            var inserted = MusicKitSelection.jukeboxDisc(context.getItemInHand(), definition);
            if (inserted.isEmpty() || !(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox))
                return InteractionResult.FAIL;
            jukebox.setFirstItem(inserted);
            if (jukebox.getFirstItem().isEmpty()) return InteractionResult.FAIL;
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(context.getPlayer(), state));
            context.getItemInHand().shrink(1);
            if (context.getPlayer() != null) context.getPlayer().awardStat(Stats.PLAY_RECORD);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
