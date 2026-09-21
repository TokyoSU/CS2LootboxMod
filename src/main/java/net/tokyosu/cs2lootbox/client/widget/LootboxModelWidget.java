package net.tokyosu.cs2lootbox.client.widget;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LegendaryLoot;
import net.tokyosu.cs2lootbox.api.lootbox.LootEntry;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.client.animation.LootboxAnimatable;
import net.tokyosu.cs2lootbox.client.animation.LootboxAnimationState;
import net.tokyosu.cs2lootbox.client.renderer.LootboxGuiRenderer;
import net.tokyosu.cs2lootbox.config.CS2LootboxClientConfig;
import net.tokyosu.cs2lootbox.config.CS2LootboxServerConfig;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import net.tokyosu.cs2lootbox.loot.LootboxLootRoller;
import net.tokyosu.cs2lootbox.loot.StatTrackUtil;
import net.tokyosu.cs2lootbox.registry.BuiltInLootboxes;
import net.tokyosu.cs2lootbox.registry.LootboxRegistry;
import net.tokyosu.cs2lootbox.registry.ModSounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Complete CS-style lootbox opening widget.
 *
 * The server owns the actual reward roll and pending ItemStack. The client only
 * animates a deterministic carousel around that already-selected result.
 */
@Configurable(name = "CS2 Lootbox Model", collapse = false)
@LDLRegister(
        name = "cs2lootbox_model",
        group = "widget.cs2lootbox",
        modID = CS2LootBoxMod.MOD_ID
)
public class LootboxModelWidget extends Widget implements IConfigurableWidget {
    private static final int ACTION_REQUEST_OPEN = 100;
    private static final int ACTION_ACCEPT_REWARD = 101;
    private static final int UPDATE_OPEN_CONFIRMED = 110;
    private static final int UPDATE_OPEN_REJECTED = 111;

    private static final long OPEN_BUTTON_LOADING_MS = 850L;
    private static final double ANIMATION_END_EPSILON_TICKS = 0.05D;
    /**
     * One client tick after GeckoLib reports the open animation has reached
     * open_idle. This guarantees the fully-open pose is rendered at least once
     * before the roulette replaces the opening presentation.
     */
    private static final long OPEN_TO_CAROUSEL_DELAY_MS = 50L;
    private static final long ROLL_DURATION_MS = 4_800L;
    private static final long SNAP_DURATION_MS = 360L;
    private static final long CLOSE_FADE_MS = 320L;
    private static final ResourceLocation RESULT_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CS2LootBoxMod.MOD_ID, "textures/gui/glow_radial.png");
    private static final int LEGENDARY_GOLD = 0xFFD85A;
    private static final int LEGENDARY_TOP = 0x4A3A0D;
    private static final int LEGENDARY_CENTER = 0xA77D17;
    private static final int LEGENDARY_BOTTOM = 0x57400C;

    private static final int CAROUSEL_SLOT_COUNT = 48;
    private static final int WINNER_TARGET_SLOT = 38;
    // CS2-like roulette cards: intentionally much larger than the inventory
    // preview cards.  The band now occupies roughly the middle two-thirds of
    // the reference screen instead of looking like a small inventory strip.
    private static final int CAROUSEL_SLOT_WIDTH = 154;
    private static final int CAROUSEL_SLOT_HEIGHT = 118;
    private static final int SLOT_SPACING = 162;
    private static final int CAROUSEL_EDGE_FADE_WIDTH = 150;
    private static final int CAROUSEL_SLOT_Y = 174;
    // The marker circle is the sharp-focus area. Carousel item artwork outside
    // it receives a small multi-tap blur, matching the CS2 focus-lens look.
    private static final int CAROUSEL_FOCUS_CLEAR_RADIUS = 54;
    private static final int CAROUSEL_FOCUS_BLUR_RANGE = 92;
    private static final int CAROUSEL_MAX_BLUR_PIXELS = 4;

    @Configurable(name = "Model scale")
    @NumberRange(range = {1, 200}, wheel = 1)
    private float modelScale = 180.0F;

    @Configurable(name = "Model X offset")
    @NumberRange(range = {-300, 300}, wheel = 1)
    private float modelOffsetX = 0.0F;

    @Configurable(name = "Model Y offset")
    @NumberRange(range = {-300, 300}, wheel = 1)
    private float modelOffsetY = -52.0F;

    @Configurable(name = "Model yaw")
    @NumberRange(range = {-360, 360}, wheel = 1)
    private float modelYaw = 180.0F;

    @Configurable(name = "Model pitch")
    @NumberRange(range = {-360, 360}, wheel = 1)
    private float modelPitch = 90.0F;

    @Configurable(name = "Model roll")
    @NumberRange(range = {-360, 360}, wheel = 1)
    private float modelRoll = 0.0F;

    private ResourceLocation lootboxId = BuiltInLootboxes.DEFAULT_ID;
    private LootboxAnimationState animationState = LootboxAnimationState.FALL;

    // Common/server transaction state.
    private ItemStack pendingReward = ItemStack.EMPTY;
    private boolean openingCommitted;
    private boolean rewardGranted;

    // Client presentation state.
    private transient ClientPhase clientPhase = ClientPhase.CASE;
    private transient boolean waitingForServer;
    private transient long phaseStartedAtMs;
    private transient long visualSeed;
    private transient ItemStack clientWinningStack = ItemStack.EMPTY;
    private transient int clientWinningEntryIndex = -1;
    private transient boolean clientWinningLegendary;
    private transient int clientWinningLegendaryIndex = -1;
    private transient int clientWinningLegendaryEntryIndex = -1;
    private transient CarouselStage carouselStage = CarouselStage.PRIMARY;
    private transient List<CarouselSlot> carouselSlots = List.of();
    private transient double rollStartOffset;
    private transient double rollRawEndOffset;
    private transient double snapStartOffset;
    private transient double snapEndOffset;
    private transient int selectedCarouselIndex = -1;
    private transient boolean acceptActionSent;
    private transient @Nullable Component clientStatus;
    private transient int lastTickedMarkerIndex = Integer.MIN_VALUE;
    private transient boolean prizeRevealSoundPlayed;
    private transient boolean prizeAwardSoundPlayed;
    private transient PrizeAction pendingPrizeAction = PrizeAction.CLOSE;
    private transient boolean inventoryOpenQueued;
    private transient boolean caseDropSoundPlayed;
    private transient boolean carouselStartPending;
    private transient long carouselReadyAtMs;

    private transient @Nullable LootboxAnimatable animatable;
    private transient @Nullable LootboxGuiRenderer renderer;
    private transient @Nullable MultiBufferSource.BufferSource isolatedBufferSource;

    /** Required by LDLib's editor/reflection factory. */
    public LootboxModelWidget() {
        this(0, 0, 280, 220);
    }

    public LootboxModelWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public @NotNull ResourceLocation getLootboxId() {
        return lootboxId;
    }

    public void setLootboxId(@NotNull ResourceLocation id) {
        this.lootboxId = Objects.requireNonNull(id, "id");
        applyPreviewTransform(definition().preview());
        this.animatable = null;
    }

    private void applyPreviewTransform(@NotNull LootboxDefinition.PreviewTransform transform) {
        modelOffsetX = transform.offsetX();
        modelOffsetY = transform.offsetY();
        modelScale = transform.scale();
        modelPitch = transform.pitch();
        modelYaw = transform.yaw();
        modelRoll = transform.roll();
    }

    /** Reset whenever a new case UI is created/re-opened. */
    public void resetAnimation() {
        animationState = LootboxAnimationState.FALL;
        pendingReward = ItemStack.EMPTY;
        openingCommitted = false;
        rewardGranted = false;

        clientPhase = ClientPhase.CASE;
        waitingForServer = false;
        phaseStartedAtMs = Util.getMillis();
        visualSeed = 0L;
        clientWinningStack = ItemStack.EMPTY;
        clientWinningEntryIndex = -1;
        clientWinningLegendary = false;
        clientWinningLegendaryIndex = -1;
        clientWinningLegendaryEntryIndex = -1;
        carouselStage = CarouselStage.PRIMARY;
        carouselSlots = List.of();
        selectedCarouselIndex = -1;
        acceptActionSent = false;
        clientStatus = null;
        lastTickedMarkerIndex = Integer.MIN_VALUE;
        prizeRevealSoundPlayed = false;
        prizeAwardSoundPlayed = false;
        pendingPrizeAction = PrizeAction.CLOSE;
        inventoryOpenQueued = false;
        caseDropSoundPlayed = false;
        carouselStartPending = false;
        carouselReadyAtMs = 0L;

        if (animatable != null) {
            animatable.setAnimationState(animationState);
        }
    }

    public @NotNull LootboxDefinition getDefinition() {
        return definition();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean shouldDrawOpeningChrome() {
        return clientPhase != ClientPhase.CASE && clientPhase != ClientPhase.CASE_LOADING;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean canStartOpeningFromChrome() {
        return clientPhase == ClientPhase.CASE
                && animationState == LootboxAnimationState.IDLE
                && !waitingForServer
                && !openingCommitted;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isOpeningFromChrome() {
        return clientPhase == ClientPhase.CASE_LOADING
                || waitingForServer
                || animationState == LootboxAnimationState.OPEN
                || carouselStartPending;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean canCloseFromChrome() {
        return clientPhase == ClientPhase.CASE && !openingCommitted && !waitingForServer;
    }

    @OnlyIn(Dist.CLIENT)
    public void startOpeningFromChrome() {
        if (!canStartOpeningFromChrome()) {
            return;
        }
        clientPhase = ClientPhase.CASE_LOADING;
        phaseStartedAtMs = Util.getMillis();
        clientStatus = null;
        playDefaultUiSound(ModSounds.BUTTON_CLICK.getId());
    }

    @OnlyIn(Dist.CLIENT)
    public void closeFromChrome() {
        if (!canCloseFromChrome()) {
            return;
        }
        playDefaultUiSound(ModSounds.BUTTON_CLICK.getId());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public @Nullable Component chromeStatus() {
        if (clientStatus != null) {
            return clientStatus;
        }
        if (clientPhase == ClientPhase.CASE_LOADING || waitingForServer) {
            return Component.translatable("cs2lootbox.case_screen.preparing");
        }
        if (animationState == LootboxAnimationState.IDLE) {
            return Component.translatable("cs2lootbox.case_screen.ready");
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean hasChromeError() {
        return clientStatus != null;
    }

    public @NotNull LootboxAnimationState getAnimationState() {
        return animationState;
    }

    /** Called by LootboxUI's close listener on both logical sides. */
    public void handleUiClosed(@NotNull Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            finalizePendingReward(serverPlayer);
            return;
        }

        // The CS2 awarded sting belongs to the moment the reward UI closes,
        // not the moment the roulette stops. The actual roll is already fixed
        // by the server, so this is presentation only.
        if (!clientWinningStack.isEmpty()) {
            playPrizeAwardSound();
        }

        // VIEW_INVENTORY must wait until the server-driven container close has
        // actually reached the client. Opening InventoryScreen earlier races
        // the incoming close-container packet, which then immediately closes
        // the inventory screen again.
        if (pendingPrizeAction == PrizeAction.VIEW_INVENTORY && !inventoryOpenQueued) {
            inventoryOpenQueued = true;
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.tell(() -> {
                if (minecraft.player != null) {
                    minecraft.setScreen(new InventoryScreen(minecraft.player));
                }
            });
        }
    }

    @Override
    public void writeInitialData(@NotNull FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        buffer.writeResourceLocation(lootboxId);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readInitialData(@NotNull FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        setLootboxId(buffer.readResourceLocation());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        ensureRenderer();
        if (!caseDropSoundPlayed) {
            caseDropSoundPlayed = true;
            playDefaultUiSound(ModSounds.CASE_DROP.getId());
        }

        if ((clientPhase == ClientPhase.CASE || clientPhase == ClientPhase.CASE_LOADING) && animatable != null) {
            LootboxDefinition.AnimationSet names = definition().animations();

            if (animationState == LootboxAnimationState.FALL
                    && animationReachedEnd(names.fall())) {
                // FALL may be a completely different length for every case.
                // Switch to IDLE only after GeckoLib reaches FALL's actual end.
                setAnimationState(LootboxAnimationState.IDLE);
            } else if (animationState == LootboxAnimationState.OPEN
                    && animationReachedEnd(names.open())) {
                // OPEN is also length-independent now. If open_idle exists the
                // next render starts it; otherwise the final OPEN frame stays
                // held while the carousel startup delay runs.
                setAnimationState(LootboxAnimationState.OPEN_IDLE);
                scheduleCarouselAfterOpen();
            }
        }

        advanceClientPhase();
        if (clientPhase == ClientPhase.ROLL || clientPhase == ClientPhase.SNAP) {
            updateCarouselTickSound();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if ((clientPhase == ClientPhase.CASE || clientPhase == ClientPhase.CASE_LOADING)
                && animationState == LootboxAnimationState.IDLE) {
            if (clientPhase == ClientPhase.CASE
                    && !waitingForServer
                    && isInsideOpenButton(mouseX, mouseY)) {
                startOpeningFromChrome();
                return true;
            }

            if (clientPhase == ClientPhase.CASE && !openingCommitted && !waitingForServer && isInsideCloseButton(mouseX, mouseY)) {
                closeUi();
                return true;
            }
        }

        if (clientPhase == ClientPhase.PRIZE) {
            if (isInsideInventoryButton(mouseX, mouseY)) {
                playDefaultUiSound(ModSounds.MENU_ACCEPT.getId());
                pendingPrizeAction = PrizeAction.VIEW_INVENTORY;
                clientPhase = ClientPhase.CLOSING;
                phaseStartedAtMs = Util.getMillis();
                acceptActionSent = false;
                return true;
            }
            if (isInsideClosePrizeButton(mouseX, mouseY)) {
                playDefaultUiSound(ModSounds.MENU_ACCEPT.getId());
                pendingPrizeAction = PrizeAction.CLOSE;
                clientPhase = ClientPhase.CLOSING;
                phaseStartedAtMs = Util.getMillis();
                acceptActionSent = false;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleClientAction(int id, @NotNull FriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);

        if (!(getGui().entityPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (id == ACTION_REQUEST_OPEN) {
            handleOpenRequest(serverPlayer);
        } else if (id == ACTION_ACCEPT_REWARD) {
            finalizePendingReward(serverPlayer);
            serverPlayer.closeContainer();
        }
    }

    private void handleOpenRequest(@NotNull ServerPlayer player) {
        if (openingCommitted || !pendingReward.isEmpty()) {
            return;
        }

        if (!CS2LootboxServerConfig.ALLOW_CASE_OPENING.get()) {
            rejectOpen("cs2lootbox.message.opening_disabled");
            return;
        }

        LootboxDefinition definition = definition();
        if (definition.loot().isEmpty() && definition.legendaryLoot().isEmpty()) {
            rejectOpen("cs2lootbox.message.no_loot");
            return;
        }

        InteractionHand hand = getHeldHand();
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof LootboxCaseItem caseItem)
                || !caseItem.getDefinition().id().equals(definition.id())) {
            rejectOpen("cs2lootbox.message.invalid_case");
            return;
        }

        Item keyItem = ForgeRegistries.ITEMS.getValue(definition.keyItemId());
        ItemStack keyStack = player.getAbilities().instabuild ? ItemStack.EMPTY : findItem(player, keyItem);
        if (!player.getAbilities().instabuild && (keyItem == null || keyStack.isEmpty())) {
            rejectOpen("cs2lootbox.message.missing_key");
            return;
        }

        RandomSource random = RandomSource.create();
        LootboxLootRoller.RollResult result = LootboxLootRoller.roll(definition, random);
        if (result == null) {
            rejectOpen("cs2lootbox.message.no_valid_loot");
            return;
        }

        if (!player.getAbilities().instabuild) {
            keyStack.shrink(1);
        }

        openingCommitted = true;
        pendingReward = result.stack().copy();
        rewardGranted = false;

        long seed = random.nextLong();
        writeUpdateInfo(UPDATE_OPEN_CONFIRMED, out -> {
            out.writeLong(seed);
            out.writeBoolean(result.legendary());
            out.writeVarInt(result.entryIndex());
            out.writeVarInt(result.legendaryIndex());
            out.writeVarInt(result.legendaryEntryIndex());
            out.writeItem(pendingReward);
        });
    }

    private void rejectOpen(@NotNull String translationKey) {
        writeUpdateInfo(UPDATE_OPEN_REJECTED, out -> out.writeUtf(translationKey));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, @NotNull FriendlyByteBuf buffer) {
        if (id == UPDATE_OPEN_CONFIRMED) {
            visualSeed = buffer.readLong();
            clientWinningLegendary = buffer.readBoolean();
            clientWinningEntryIndex = buffer.readVarInt();
            clientWinningLegendaryIndex = buffer.readVarInt();
            clientWinningLegendaryEntryIndex = buffer.readVarInt();
            clientWinningStack = buffer.readItem();
            waitingForServer = false;
            clientPhase = ClientPhase.CASE;
            openingCommitted = true;
            carouselStartPending = false;
            carouselReadyAtMs = 0L;
            clientStatus = null;
            playOpenSound();
            setAnimationState(LootboxAnimationState.OPEN);
            return;
        }

        if (id == UPDATE_OPEN_REJECTED) {
            waitingForServer = false;
            clientPhase = ClientPhase.CASE;
            clientStatus = Component.translatable(buffer.readUtf());
            playDefaultUiSound(ModSounds.MENU_INVALID.getId());
            return;
        }

        super.readUpdateInfo(id, buffer);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        ensureRenderer();
        advanceClientPhase();

        switch (clientPhase) {
            case CASE, CASE_LOADING -> drawCase(graphics, mouseX, mouseY, partialTicks);
            case ROLL, SNAP -> drawCarousel(graphics, mouseX, mouseY, partialTicks);
            case PRIZE, CLOSING -> drawPrize(graphics, mouseX, mouseY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawCase(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderConfiguredCasePreview(graphics, partialTicks);
    }

    /**
     * Render the case using the definition's preview transform exactly as configured.
     * Both the normal case phase and the carousel call this same method so changing
     * position/scale/rotation cannot make the case jump when the roulette starts.
     */
    @OnlyIn(Dist.CLIENT)
    private void renderConfiguredCasePreview(@NotNull GuiGraphics graphics, float partialTicks) {
        Position position = getPosition();
        Size size = getSize();
        int centerX = position.x + size.width / 2;
        int footerTop = footerTop();
        int viewportTop = position.y + 88;
        int viewportBottom = footerTop - 18;
        float caseViewportHeight = Math.max(120.0F, viewportBottom - viewportTop);

        float modelCenterX = centerX + modelOffsetX;
        float modelCenterY = viewportTop + caseViewportHeight / 2.0F + modelOffsetY;

        renderPreviewModel(graphics, modelCenterX, modelCenterY, modelScale, 200.0F, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    private void renderPreviewModel(@NotNull GuiGraphics graphics, float centerX, float centerY, float scale, float z, float partialTicks) {
        graphics.flush();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.clearDepth(1.0D);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

            poseStack.translate(centerX, centerY, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(modelPitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(modelYaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(modelRoll));
            poseStack.scale(scale, -scale, scale);

            MultiBufferSource.BufferSource bufferSource = getIsolatedBufferSource();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            RenderType renderType = renderer.getRenderType(
                    animatable,
                    renderer.getTextureLocation(animatable),
                    bufferSource,
                    partialTicks
            );
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

            LootboxDefinition.PreviewTransform preview = definition().preview();
            renderer.render(
                    poseStack,
                    animatable,
                    bufferSource,
                    renderType,
                    vertexConsumer,
                    packLight(preview.blockLight(), preview.skyLight())
            );

            // Flush every buffer the GeckoLib pass may have touched before any
            // 2D roulette/UI is submitted. This prevents a delayed model batch
            // from being drawn over the carousel later in the frame.
            bufferSource.endBatch();
            graphics.flush();
        } finally {
            poseStack.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    private void scheduleCarouselAfterOpen() {
        if (carouselStartPending || clientPhase != ClientPhase.CASE) {
            return;
        }

        carouselStartPending = true;
        carouselReadyAtMs = Util.getMillis() + OPEN_TO_CAROUSEL_DELAY_MS;
    }

    @OnlyIn(Dist.CLIENT)
    private void beginCarousel() {
        carouselStartPending = false;
        carouselReadyAtMs = 0L;
        beginPrimaryCarousel();
    }

    @OnlyIn(Dist.CLIENT)
    private void beginPrimaryCarousel() {
        if (clientWinningStack.isEmpty()) {
            return;
        }
        if (clientWinningLegendary) {
            if (clientWinningLegendaryIndex < 0 || clientWinningLegendaryIndex >= definition().legendaryLoot().size()) {
                return;
            }
        } else if (clientWinningEntryIndex < 0 || clientWinningEntryIndex >= definition().loot().size()) {
            return;
        }

        carouselStage = CarouselStage.PRIMARY;
        LootboxDefinition definition = definition();
        RandomSource random = RandomSource.create(visualSeed);
        List<CarouselSlot> slots = new ArrayList<>(CAROUSEL_SLOT_COUNT);

        // The server roll is a true independent percentage check. The cosmetic
        // strip intentionally uses a low-discrepancy accumulator instead of 48
        // independent legendary checks. This keeps the long-term visual density
        // equal to the configured chance while preventing rare gold panels from
        // clustering unrealistically in one carousel.
        //
        // Example: legendary(1.0) averages one gold filler every 100 slots. A
        // 48-slot strip can therefore contain at most one scheduled filler gold
        // panel (unless the authoritative winning slot itself is legendary).
        double legendaryRate = LootboxLootRoller.totalLegendaryChance(definition.legendaryLoot()) / 100.0D;
        double legendaryAccumulator = legendaryRate > 0.0D ? random.nextDouble() : 0.0D;

        for (int i = 0; i < CAROUSEL_SLOT_COUNT; i++) {
            boolean showLegendary = false;
            if (legendaryRate > 0.0D) {
                legendaryAccumulator += legendaryRate;
                if (legendaryAccumulator >= 1.0D) {
                    legendaryAccumulator -= 1.0D;
                    showLegendary = true;
                }
            }

            if (showLegendary) {
                int legendaryIndex = LootboxLootRoller.pickLegendaryPanelIndex(definition.legendaryLoot(), random);
                if (legendaryIndex >= 0) {
                    slots.add(CarouselSlot.legendary(legendaryIndex));
                    continue;
                }
            }

            int normalIndex = LootboxLootRoller.pickEntryIndex(definition.loot(), random);
            if (normalIndex < 0) {
                slots.add(CarouselSlot.empty());
                continue;
            }

            LootEntry entry = definition.loot().get(normalIndex);
            slots.add(CarouselSlot.item(entry, LootboxLootRoller.createStack(entry, random)));
        }

        if (clientWinningLegendary) {
            slots.set(WINNER_TARGET_SLOT, CarouselSlot.legendary(clientWinningLegendaryIndex));
        } else {
            LootEntry winning = definition.loot().get(clientWinningEntryIndex);
            slots.set(WINNER_TARGET_SLOT, CarouselSlot.item(winning, clientWinningStack.copy()));
        }

        prepareCarousel(slots, random);
    }

    @OnlyIn(Dist.CLIENT)
    private void beginLegendaryCarousel() {
        if (clientWinningLegendaryIndex < 0 || clientWinningLegendaryIndex >= definition().legendaryLoot().size()) {
            showPrize();
            return;
        }

        LegendaryLoot legendary = definition().legendaryLoot().get(clientWinningLegendaryIndex);
        if (clientWinningLegendaryEntryIndex < 0 || clientWinningLegendaryEntryIndex >= legendary.subLoot().size()) {
            showPrize();
            return;
        }

        carouselStage = CarouselStage.LEGENDARY;
        RandomSource random = RandomSource.create(visualSeed ^ 0x6A09E667F3BCC909L);
        List<CarouselSlot> slots = new ArrayList<>(CAROUSEL_SLOT_COUNT);

        for (int i = 0; i < CAROUSEL_SLOT_COUNT; i++) {
            int entryIndex = LootboxLootRoller.pickEntryIndex(legendary.subLoot(), random);
            if (entryIndex < 0) {
                slots.add(CarouselSlot.empty());
                continue;
            }
            LootEntry entry = legendary.subLoot().get(entryIndex);
            slots.add(CarouselSlot.item(entry, LootboxLootRoller.createStack(entry, random)));
        }

        LootEntry winning = legendary.subLoot().get(clientWinningLegendaryEntryIndex);
        slots.set(WINNER_TARGET_SLOT, CarouselSlot.item(winning, clientWinningStack.copy()));
        prepareCarousel(slots, random);
    }

    @OnlyIn(Dist.CLIENT)
    private void prepareCarousel(@NotNull List<CarouselSlot> slots, @NotNull RandomSource random) {
        carouselSlots = List.copyOf(slots);

        int markerLocalX = getSize().width / 2;
        rollStartOffset = markerLocalX + SLOT_SPACING * 3.5D;

        double landingJitter = (random.nextDouble() * 0.90D) - 0.45D;
        rollRawEndOffset = markerLocalX - (WINNER_TARGET_SLOT + landingJitter) * SLOT_SPACING;

        selectedCarouselIndex = -1;
        lastTickedMarkerIndex = Integer.MIN_VALUE;
        prizeRevealSoundPlayed = false;
        clientPhase = ClientPhase.ROLL;
        phaseStartedAtMs = Util.getMillis();
    }

    @OnlyIn(Dist.CLIENT)
    private void showPrize() {
        clientPhase = ClientPhase.PRIZE;
        phaseStartedAtMs = Util.getMillis();
        playPrizeRevealSound();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawCarousel(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Position position = getPosition();
        Size size = getSize();
        int viewportX = fullscreenReferenceLeft();
        int viewportRight = fullscreenReferenceRight();
        int viewportW = Math.max(1, viewportRight - viewportX);
        int slotY = position.y + CAROUSEL_SLOT_Y;
        int markerX = position.x + size.width / 2;
        int markerY = slotY + CAROUSEL_SLOT_HEIGHT / 2;

        float alpha = 1.0F;

        // Draw the 3D case first. Previously the header/footer were submitted
        // before this GeckoLib pass, so the case could visually pass through
        // the header text even though the roulette itself was on a higher layer.
        renderConfiguredCasePreview(graphics, partialTicks);
        graphics.flush();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack roulettePose = graphics.pose();
        roulettePose.pushPose();
        roulettePose.translate(0.0F, 0.0F, 500.0F);
        try {
            // Only the roulette BACKGROUND is translucent.  The cards/items
            // themselves stay opaque in the center (then fade at the left/right
            // edges) so they remain readable over the 3D case.
            final float rouletteBackgroundAlpha = alpha * 0.72F;

            int focusRadius = Math.min(180, Math.max(120, (int) (size.height * 0.34F)));
            drawFilledCircle(graphics, markerX, markerY + 8, focusRadius,
                    withAlpha(0x000000, rouletteBackgroundAlpha * 0.34F));

            graphics.fill(viewportX, slotY - 10, viewportX + viewportW, slotY + CAROUSEL_SLOT_HEIGHT + 10,
                    withAlpha(0x080808, rouletteBackgroundAlpha * 0.34F));

            double offset = currentCarouselOffset();

            for (int i = 0; i < carouselSlots.size(); i++) {
                CarouselSlot slot = carouselSlots.get(i);
                int centerX = position.x + (int) Math.round(offset + i * SLOT_SPACING);
                int left = centerX - CAROUSEL_SLOT_WIDTH / 2;

                if (left > viewportX + viewportW || left + CAROUSEL_SLOT_WIDTH < viewportX) {
                    continue;
                }

                float edgeAlpha = carouselEdgeAlpha(centerX, viewportX, viewportW);
                float slotAlpha = alpha * edgeAlpha;
                if (slotAlpha <= 0.01F) {
                    continue;
                }

                float focusBlur = carouselFocusBlur(centerX, markerX);
                drawCarouselSlot(graphics, slot, left, slotY, slotAlpha, focusBlur);
            }

            // The side veil remains translucent as well, so cards fade naturally
            // into the background instead of being hard-clipped.
            drawCarouselEdgeFades(graphics, viewportX, viewportW, slotY - 10,
                    CAROUSEL_SLOT_HEIGHT + 20, rouletteBackgroundAlpha);

            drawMarkerOverlay(graphics, markerX, markerY, slotY, alpha);

            graphics.flush();
        } finally {
            roulettePose.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        // Final UI/chrome pass. Keep it above BOTH the GeckoLib case (z=200)
        // and the roulette cards (z=500), so neither can overlap the header text
        // or footer controls.
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        PoseStack chromePose = graphics.pose();
        chromePose.pushPose();
        chromePose.translate(0.0F, 0.0F, 700.0F);
        try {
            drawHeader(graphics, alpha);
            drawFooterControls(graphics, mouseX, mouseY, alpha, true, false);
            graphics.flush();
        } finally {
            chromePose.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawCarouselSlot(@NotNull GuiGraphics graphics, @NotNull CarouselSlot slot, int x, int y, float alpha, float focusBlur) {
        if (slot.legendaryIndex() >= 0) {
            drawLegendaryCarouselSlot(graphics, slot.legendaryIndex(), x, y, alpha, focusBlur);
            return;
        }

        LootEntry entry = slot.entry();
        int rarity = entry == null ? 0xB8B8B8 : rarityColor(entry, slot.stack());
        boolean special = entry != null && "special".equals(rarityTier(entry, slot.stack()));
        if (special) {
            int middleY = y + CAROUSEL_SLOT_HEIGHT / 2;
            graphics.fillGradient(x, y, x + CAROUSEL_SLOT_WIDTH, middleY,
                    withAlpha(LEGENDARY_TOP, alpha), withAlpha(LEGENDARY_CENTER, alpha));
            graphics.fillGradient(x, middleY, x + CAROUSEL_SLOT_WIDTH, y + CAROUSEL_SLOT_HEIGHT,
                    withAlpha(LEGENDARY_CENTER, alpha), withAlpha(LEGENDARY_BOTTOM, alpha));
            if (CS2LootboxClientConfig.ENABLE_RARITY_GLOWS.get()) {
                blitTinted(graphics, RESULT_GLOW_TEXTURE,
                        x + 19, y + 2, CAROUSEL_SLOT_WIDTH - 38, CAROUSEL_SLOT_HEIGHT - 12,
                        LEGENDARY_GOLD, alpha * 0.48F);
            }
        } else {
            graphics.fill(x, y, x + CAROUSEL_SLOT_WIDTH, y + CAROUSEL_SLOT_HEIGHT,
                    withAlpha(0x202020, alpha));
            graphics.fill(x + 1, y + 1, x + CAROUSEL_SLOT_WIDTH - 1, y + CAROUSEL_SLOT_HEIGHT - 1,
                    withAlpha(0x343434, alpha));
        }
        drawTexturedSlotBorder(graphics, x, y, CAROUSEL_SLOT_WIDTH, CAROUSEL_SLOT_HEIGHT, rarity, alpha);
        graphics.fill(x + 1, y + CAROUSEL_SLOT_HEIGHT - 5,
                x + CAROUSEL_SLOT_WIDTH - 1, y + CAROUSEL_SLOT_HEIGHT - 1,
                withAlpha(rarity, alpha));

        if (!slot.stack().isEmpty()) {
            renderCarouselItem(graphics, slot.stack(),
                    x + CAROUSEL_SLOT_WIDTH / 2, y + CAROUSEL_SLOT_HEIGHT / 2 - 3,
                    3.0F, alpha, focusBlur);
            if (slot.stack().getCount() > 1) {
                String count = Integer.toString(slot.stack().getCount());
                Font font = Minecraft.getInstance().font;
                graphics.drawString(font, count,
                        x + CAROUSEL_SLOT_WIDTH - 5 - font.width(count),
                        y + CAROUSEL_SLOT_HEIGHT - 16,
                        withAlpha(0xFFFFFF, alpha), true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawLegendaryCarouselSlot(@NotNull GuiGraphics graphics, int legendaryIndex, int x, int y, float alpha, float focusBlur) {
        if (legendaryIndex < 0 || legendaryIndex >= definition().legendaryLoot().size()) {
            return;
        }

        LegendaryLoot legendary = definition().legendaryLoot().get(legendaryIndex);
        int middleY = y + CAROUSEL_SLOT_HEIGHT / 2;
        graphics.fillGradient(x, y, x + CAROUSEL_SLOT_WIDTH, middleY,
                withAlpha(LEGENDARY_TOP, alpha), withAlpha(LEGENDARY_CENTER, alpha));
        graphics.fillGradient(x, middleY, x + CAROUSEL_SLOT_WIDTH, y + CAROUSEL_SLOT_HEIGHT,
                withAlpha(LEGENDARY_CENTER, alpha), withAlpha(LEGENDARY_BOTTOM, alpha));

        if (CS2LootboxClientConfig.ENABLE_RARITY_GLOWS.get()) {
            blitTinted(graphics, RESULT_GLOW_TEXTURE,
                    x + 19, y + 2, CAROUSEL_SLOT_WIDTH - 38, CAROUSEL_SLOT_HEIGHT - 12,
                    LEGENDARY_GOLD, alpha * 0.58F);
        }

        ResourceLocation foreground = resolveLegendaryForeground(legendary.foreground());
        int iconWidth = 116;
        int iconHeight = 90;
        blitCarouselTexture(graphics, foreground,
                x + (CAROUSEL_SLOT_WIDTH - iconWidth) / 2,
                y + (CAROUSEL_SLOT_HEIGHT - iconHeight) / 2 - 2,
                iconWidth, iconHeight, alpha, focusBlur);

        drawTexturedSlotBorder(graphics, x, y, CAROUSEL_SLOT_WIDTH, CAROUSEL_SLOT_HEIGHT, LEGENDARY_GOLD, alpha);
        graphics.fill(x + 1, y + CAROUSEL_SLOT_HEIGHT - 5,
                x + CAROUSEL_SLOT_WIDTH - 1, y + CAROUSEL_SLOT_HEIGHT - 1,
                withAlpha(LEGENDARY_GOLD, alpha));
    }

    private static float carouselFocusBlur(int contentCenterX, int markerX) {
        float outside = Math.abs(contentCenterX - markerX) - CAROUSEL_FOCUS_CLEAR_RADIUS;
        if (outside <= 0.0F) {
            return 0.0F;
        }

        float t = Math.max(0.0F, Math.min(1.0F, outside / CAROUSEL_FOCUS_BLUR_RANGE));
        // Smoothstep keeps the focus transition from popping as an item crosses
        // the circular marker boundary.
        return t * t * (3.0F - 2.0F * t);
    }

    private static float carouselEdgeAlpha(int centerX, int viewportX, int viewportW) {
        float leftDistance = centerX - viewportX;
        float rightDistance = viewportX + viewportW - centerX;
        float distance = Math.min(leftDistance, rightDistance);
        float t = Math.max(0.0F, Math.min(1.0F, distance / CAROUSEL_EDGE_FADE_WIDTH));
        // Smoothstep: no visible hard threshold when a card enters/leaves.
        return t * t * (3.0F - 2.0F * t);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawCarouselEdgeFades(
            @NotNull GuiGraphics graphics, int viewportX, int viewportW, int y, int height, float alpha) {
        int steps = 36;
        int fadeWidth = Math.min(CAROUSEL_EDGE_FADE_WIDTH, viewportW / 3);
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            int x0 = Math.round(fadeWidth * t0);
            int x1 = Math.max(x0 + 1, Math.round(fadeWidth * t1));
            float veil = (1.0F - t0);
            veil = veil * veil * 0.58F * alpha;
            int color = withAlpha(0x050505, veil);
            graphics.fill(viewportX + x0, y, viewportX + x1, y + height, color);
            graphics.fill(viewportX + viewportW - x1, y, viewportX + viewportW - x0, y + height, color);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawPrize(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;

        float alpha = clientPhase == ClientPhase.CLOSING
                ? 1.0F - phaseProgress(CLOSE_FADE_MS)
                : 1.0F;

        LootEntry entry = winningEntry();
        int rarity = entry == null ? 0xB8B8B8 : rarityColor(entry, clientWinningStack);

        int bgLeft = prizePanelLeft();
        int bgTop = prizePanelTop();
        int bgRight = prizePanelRight();
        int bgBottom = prizePanelBottom();
        int panelWidth = bgRight - bgLeft;
        int panelHeight = bgBottom - bgTop;

        int itemCenterX = bgLeft + Math.round(panelWidth * 0.69F);
        int itemCenterY = bgTop + 104;
        // Keep the glow centered behind the item icon, like CS2, while still
        // clipping it to the outer result-card bounds.
        int burstX = itemCenterX;
        int burstY = itemCenterY;
        int textLeft = bgLeft + 34;
        int nameY = bgTop + 170;
        int bottomLineY = bgBottom - 29;
        int stripColor = withAlpha(rarity, alpha * 0.96F);

        graphics.fill(fullscreenReferenceLeft(), position.y, fullscreenReferenceRight(), position.y + size.height,
                withAlpha(0x070911, alpha * 0.36F));
        graphics.fill(bgLeft, bgTop, bgRight, bgBottom, withAlpha(0x1D2550, alpha));
        graphics.fill(bgLeft, bgTop, bgLeft + 6, bgBottom, stripColor);
        drawBorder(graphics, bgLeft, bgTop, panelWidth, panelHeight, withAlpha(0xD8E0FF, alpha * 0.12F));
        graphics.fill(bgLeft + 18, bottomLineY, bgRight - 18, bottomLineY + 1, withAlpha(0xA7B3DF, alpha * 0.24F));

        // CS2-style reward glow: two copies of the supplied radial texture.
        // Drive the glow directly from the OUTER panel bounds so it fills the
        // whole result card area instead of behaving like a smaller inner box.
        if (CS2LootboxClientConfig.ENABLE_RARITY_GLOWS.get()) {
            drawClippedPrizeGlow(graphics, RESULT_GLOW_TEXTURE,
                    burstX, burstY,
                    bgLeft, bgTop, bgRight, bgBottom,
                    rarity, alpha);
        }

        graphics.drawString(font, Component.translatable("cs2lootbox.prize.new_item"),
                bgLeft + 34, bgTop + 16, withAlpha(0x4C62D9, alpha), true);

        float itemScale = 4.35F * (0.93F + 0.07F * alpha);
        renderItemScaled(graphics, clientWinningStack, itemCenterX, itemCenterY, itemScale, alpha);

        Component name = prizeName(entry, clientWinningStack);
        graphics.drawString(font, name, textLeft, nameY, withAlpha(0xFFFFFF, alpha), true);

        ResourceLocation collectionIcon = definition().resultCollectionIconTexture();
        Component collectionLabel = prizeCollectionLabel();

        // CS2-like collection row: keep it a little higher and render the
        // emblem larger so 128x128 / 512x512 collection images stay readable.
        int collectionTop = nameY + 16;
        int collectionIconSize = 40;
        int collectionTextX = textLeft;
        int collectionTextY = collectionTop + (collectionIconSize - font.lineHeight) / 2;

        if (collectionIcon != null) {
            blitPlain(graphics, collectionIcon,
                    textLeft, collectionTop,
                    collectionIconSize, collectionIconSize,
                    alpha);
            collectionTextX += collectionIconSize + 10;
        }

        graphics.drawString(font, collectionLabel,
                collectionTextX, collectionTextY,
                withAlpha(0xDADADA, alpha), false);

        if (clientPhase == ClientPhase.PRIZE) {
            int invX = inventoryButtonX();
            int invY = prizeButtonsY();
            int invW = inventoryButtonWidth();
            int invH = prizeButtonHeight();
            boolean invHover = mouseX >= invX && mouseX < invX + invW && mouseY >= invY && mouseY < invY + invH;
            graphics.fill(invX, invY, invX + invW, invY + invH,
                    withAlpha(invHover ? 0x1F2749 : 0x18203C, alpha));
            drawBorder(graphics, invX, invY, invW, invH, withAlpha(0xCCD6FF, alpha * 0.75F));
            drawCentered(graphics, font, Component.translatable("cs2lootbox.prize.view_inventory"),
                    invX + invW / 2, invY + 7, withAlpha(0xFFFFFF, alpha));

            int closeX = closePrizeButtonX();
            int closeW = closePrizeButtonWidth();
            boolean closeHover = mouseX >= closeX && mouseX < closeX + closeW && mouseY >= invY && mouseY < invY + invH;
            graphics.fill(closeX, invY, closeX + closeW, invY + invH,
                    withAlpha(closeHover ? 0x2A2A2A : 0x202020, alpha));
            drawBorder(graphics, closeX, invY, closeW, invH, withAlpha(0xB7B7B7, alpha * 0.75F));
            drawCentered(graphics, font, Component.translatable("cs2lootbox.prize.close"),
                    closeX + closeW / 2, invY + 7, withAlpha(0xFFFFFF, alpha));
        }

        if (clientPhase == ClientPhase.CLOSING) {
            float cover = 1.0F - alpha;
            graphics.fill(fullscreenReferenceLeft(), position.y, fullscreenReferenceRight(), position.y + size.height,
                    withAlpha(0x000000, cover));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void advanceClientPhase() {
        long now = Util.getMillis();
        long elapsed = now - phaseStartedAtMs;

        if (carouselStartPending
                && clientPhase == ClientPhase.CASE
                && animationState == LootboxAnimationState.OPEN_IDLE
                && now >= carouselReadyAtMs) {
            beginCarousel();
            return;
        }

        if (clientPhase == ClientPhase.CASE_LOADING && elapsed >= OPEN_BUTTON_LOADING_MS && !waitingForServer) {
            waitingForServer = true;
            writeClientAction(ACTION_REQUEST_OPEN, buffer -> {
            });
            return;
        }

        if (clientPhase == ClientPhase.ROLL && elapsed >= ROLL_DURATION_MS) {
            int markerLocalX = getSize().width / 2;
            double localIndex = (markerLocalX - rollRawEndOffset) / SLOT_SPACING;
            // Exactly N + 0.5 becomes N + 1: right-hand slot wins ties.
            selectedCarouselIndex = clampCarouselIndex((int) Math.floor(localIndex + 0.5D));
            snapStartOffset = rollRawEndOffset;
            snapEndOffset = markerLocalX - selectedCarouselIndex * (double) SLOT_SPACING;
            clientPhase = ClientPhase.SNAP;
            phaseStartedAtMs = Util.getMillis();
            return;
        }

        if (clientPhase == ClientPhase.SNAP && elapsed >= SNAP_DURATION_MS) {
            if (carouselStage == CarouselStage.PRIMARY && clientWinningLegendary) {
                // CS2 uses the Legendary/Classified awarded sting for the gold
                // rare-special hit before the second, legendary-only carousel.
                playDefaultUiSound(ModSounds.CASE_AWARDED_LEGENDARY.getId());
                beginLegendaryCarousel();
            } else {
                showPrize();
            }
            return;
        }

        if (clientPhase == ClientPhase.CLOSING && elapsed >= CLOSE_FADE_MS && !acceptActionSent) {
            acceptActionSent = true;
            PrizeAction action = pendingPrizeAction;

            // Tell the server to commit the pending reward and close the
            // lootbox container first. The server-side reward path is still
            // authoritative; opening the inventory here is only UI navigation.
            writeClientAction(ACTION_ACCEPT_REWARD, out -> {
            });

        }
    }

    @OnlyIn(Dist.CLIENT)
    private double currentCarouselOffset() {
        if (clientPhase == ClientPhase.ROLL) {
            double t = phaseProgress(ROLL_DURATION_MS);
            double eased = 1.0D - Math.pow(1.0D - t, 5.0D);
            return lerp(rollStartOffset, rollRawEndOffset, eased);
        }

        if (clientPhase == ClientPhase.SNAP) {
            double t = phaseProgress(SNAP_DURATION_MS);
            double eased = 1.0D - Math.pow(1.0D - t, 3.0D);
            return lerp(snapStartOffset, snapEndOffset, eased);
        }

        return snapEndOffset;
    }

    private void finalizePendingReward(@NotNull ServerPlayer player) {
        if (!openingCommitted || rewardGranted || pendingReward.isEmpty()) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            consumeOneCase(player);
        }

        ItemStack reward = pendingReward.copy();
        player.getInventory().add(reward);
        if (!reward.isEmpty()) {
            player.drop(reward, false);
        }

        pendingReward = ItemStack.EMPTY;
        rewardGranted = true;
    }

    private void consumeOneCase(@NotNull ServerPlayer player) {
        InteractionHand hand = getHeldHand();
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof LootboxCaseItem caseItem
                && caseItem.getDefinition().id().equals(lootboxId)
                && !held.isEmpty()) {
            held.shrink(1);
            return;
        }

        Item caseItem = ForgeRegistries.ITEMS.getValue(definition().caseItemId());
        ItemStack elsewhere = findItem(player, caseItem);
        if (!elsewhere.isEmpty()) {
            elsewhere.shrink(1);
        }
    }

    private @NotNull InteractionHand getHeldHand() {
        if (getGui().holder instanceof HeldItemUIFactory.HeldItemHolder holder) {
            return holder.getHand();
        }
        return InteractionHand.MAIN_HAND;
    }

    private static @NotNull ItemStack findItem(@NotNull ServerPlayer player, @Nullable Item item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private @NotNull LootboxDefinition definition() {
        return LootboxRegistry.getOrDefault(lootboxId);
    }

    private @Nullable LootEntry winningEntry() {
        LootboxDefinition definition = definition();
        if (clientWinningLegendary) {
            if (clientWinningLegendaryIndex < 0 || clientWinningLegendaryIndex >= definition.legendaryLoot().size()) {
                return null;
            }
            LegendaryLoot legendary = definition.legendaryLoot().get(clientWinningLegendaryIndex);
            if (clientWinningLegendaryEntryIndex < 0 || clientWinningLegendaryEntryIndex >= legendary.subLoot().size()) {
                return null;
            }
            return legendary.subLoot().get(clientWinningLegendaryEntryIndex);
        }

        if (clientWinningEntryIndex < 0 || clientWinningEntryIndex >= definition.loot().size()) {
            return null;
        }
        return definition.loot().get(clientWinningEntryIndex);
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component prizeName(@Nullable LootEntry entry, @NotNull ItemStack stack) {
        // If KubeJS supplied an explicit prize name, keep using that text in the
        // result panel and add the same StatTrak prefix used by the ItemStack.
        if (entry != null && entry.nameTranslationKey() != null) {
            Component configuredName = Component.translatable(entry.nameTranslationKey());
            if (!stack.isEmpty() && StatTrackUtil.isStatTrack(stack)) {
                return StatTrackUtil.createDisplayNamePrefix(stack)
                        .append(" ")
                        .append(configuredName.copy());
            }
            return configuredName;
        }

        // Otherwise the real ItemStack name is authoritative. StatTrackUtil
        // already prefixed it at server mutation time, so do not prefix twice.
        return stack.isEmpty() ? Component.empty() : stack.getHoverName();
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component prizeCollectionLabel() {
        String translationKey = definition().resultCollectionTranslationKey();
        if (translationKey != null && !translationKey.isBlank()) {
            if (I18n.exists(translationKey)) {
                return Component.translatable(translationKey);
            }
            return Component.literal(translationKey);
        }
        return caseDisplayName();
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component prizeRarity(@Nullable LootEntry entry, @NotNull ItemStack stack) {
        if (entry != null && entry.rarityTranslationKey() != null) {
            return Component.translatable(entry.rarityTranslationKey());
        }

        Rarity rarity = stack.isEmpty() ? Rarity.COMMON : stack.getRarity();
        return Component.translatable("cs2lootbox.rarity." + rarity.name().toLowerCase(Locale.ROOT));
    }

    private static int rarityColor(@Nullable LootEntry entry, @NotNull ItemStack stack) {
        if (entry != null && entry.rarityColor() != -1) {
            return entry.rarityColor() & 0xFFFFFF;
        }

        String tier = rarityTier(entry, stack);
        return switch (tier) {
            case "industrial" -> 0x5E98D9;
            case "milspec" -> 0x4B69FF;
            case "restricted" -> 0x8847FF;
            case "classified" -> 0xD32CE6;
            case "covert" -> 0xEB4B4B;
            case "special" -> 0xFFD700;
            default -> 0xB0C3D9;
        };
    }

    private void setAnimationState(@NotNull LootboxAnimationState newState) {
        if (animationState == newState) {
            return;
        }
        animationState = newState;
        if (animatable != null) {
            animatable.setAnimationState(newState);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private boolean animationReachedEnd(@NotNull String expectedStage) {
        if (animatable == null || !expectedStage.equals(animatable.getCurrentAnimationStage())) {
            return false;
        }

        double animationLength = animatable.getCurrentAnimationLength();
        double currentFrame = animatable.getCurrentAnimationTick();
        if (!(animationLength > 0.0D)
                || !Double.isFinite(animationLength)
                || !Double.isFinite(currentFrame)) {
            return false;
        }

        // No per-case frame counts or wall-clock duration assumptions.
        // GeckoLib supplies both values in its own animation tick units.
        return currentFrame >= animationLength - ANIMATION_END_EPSILON_TICKS;
    }

    @OnlyIn(Dist.CLIENT)
    private void playOpenSound() {
        LootboxDefinition definition = definition();
        playUiSound(definition.openSound(), definition.soundProfile().openResolved());
    }

    @OnlyIn(Dist.CLIENT)
    private void ensureRenderer() {
        if (animatable == null) {
            animatable = new LootboxAnimatable(definition());
            animatable.setAnimationState(animationState);
        }
        if (renderer == null) {
            renderer = new LootboxGuiRenderer();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull MultiBufferSource.BufferSource getIsolatedBufferSource() {
        if (isolatedBufferSource == null) {
            isolatedBufferSource = MultiBufferSource.immediate(new BufferBuilder(4096));
        }
        return isolatedBufferSource;
    }

    @OnlyIn(Dist.CLIENT)
    private void renderCarouselItem(
            @NotNull GuiGraphics graphics,
            @NotNull ItemStack stack,
            int centerX,
            int centerY,
            float scale,
            float alpha,
            float blur) {
        float amount = Math.max(0.0F, Math.min(1.0F, blur));
        if (amount <= 0.01F) {
            renderItemScaled(graphics, stack, centerX, centerY, scale, alpha);
            return;
        }

        int radius = Math.max(1, Math.round(CAROUSEL_MAX_BLUR_PIXELS * amount));
        float tapAlpha = alpha * (0.08F * amount);

        // Eight low-alpha taps form a small Gaussian-like smear without using
        // Minecraft's full-screen post-processing chain (which would blur the
        // marker/UI too). Only the carousel artwork is affected.
        renderItemScaled(graphics, stack, centerX - radius, centerY, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX + radius, centerY, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX, centerY - radius, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX, centerY + radius, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX - radius, centerY - radius, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX + radius, centerY - radius, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX - radius, centerY + radius, scale, tapAlpha);
        renderItemScaled(graphics, stack, centerX + radius, centerY + radius, scale, tapAlpha);

        // Keep a reduced sharp core so pixel-art items remain recognizable.
        renderItemScaled(graphics, stack, centerX, centerY, scale, alpha * (1.0F - 0.65F * amount));
    }

    @OnlyIn(Dist.CLIENT)
    private void blitCarouselTexture(
            @NotNull GuiGraphics graphics,
            @Nullable ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            float alpha,
            float blur) {
        float amount = Math.max(0.0F, Math.min(1.0F, blur));
        if (amount <= 0.01F) {
            blitPlain(graphics, texture, x, y, width, height, alpha);
            return;
        }

        int radius = Math.max(1, Math.round(CAROUSEL_MAX_BLUR_PIXELS * amount));
        float tapAlpha = alpha * (0.08F * amount);
        blitPlain(graphics, texture, x - radius, y, width, height, tapAlpha);
        blitPlain(graphics, texture, x + radius, y, width, height, tapAlpha);
        blitPlain(graphics, texture, x, y - radius, width, height, tapAlpha);
        blitPlain(graphics, texture, x, y + radius, width, height, tapAlpha);
        blitPlain(graphics, texture, x - radius, y - radius, width, height, tapAlpha);
        blitPlain(graphics, texture, x + radius, y - radius, width, height, tapAlpha);
        blitPlain(graphics, texture, x - radius, y + radius, width, height, tapAlpha);
        blitPlain(graphics, texture, x + radius, y + radius, width, height, tapAlpha);
        blitPlain(graphics, texture, x, y, width, height, alpha * (1.0F - 0.65F * amount));
    }

    @OnlyIn(Dist.CLIENT)
    private void renderItemScaled(
            @NotNull GuiGraphics graphics,
            @NotNull ItemStack stack,
            int centerX,
            int centerY,
            float scale,
            float alpha) {
        if (stack.isEmpty()) {
            return;
        }

        PoseStack pose = graphics.pose();

        /*
         * Let Minecraft draw the ItemStack exactly like an inventory item.
         *
         * GuiGraphics#renderItem routes through ItemRenderer with GUI display
         * context, so it naturally supports:
         *  - vanilla enchanted foil/glint;
         *  - NBT-driven item models;
         *  - custom item renderers;
         *  - GeckoLib cases/weapons that use builtin/entity.
         *
         * The roulette itself is a depth-disabled UI layer, therefore give each
         * item a clean temporary depth buffer before calling the vanilla path.
         */
        graphics.flush();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clearDepth(1.0D);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        pose.pushPose();
        try {
            float size = 16.0F * scale;
            pose.translate(centerX - size / 2.0F, centerY - size / 2.0F, 120.0F);
            pose.scale(scale, scale, 1.0F);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

            /*
             * GuiGraphics#drawManaged is deprecated. Use the same explicit
             * boundary instead: flush -> vanilla item render -> flush.
             *
             * The second flush completes all fixed foil/glint buffers before
             * we restore the roulette/result UI render state.
             */
            graphics.flush();
            graphics.renderItem(stack, 0, 0);
            graphics.flush();
        } finally {
            pose.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }


    @OnlyIn(Dist.CLIENT)
    private void updateCarouselTickSound() {
        if (carouselSlots.isEmpty()) {
            return;
        }
        int markerLocalX = getSize().width / 2;
        double localIndex = (markerLocalX - currentCarouselOffset()) / SLOT_SPACING;
        int nearest = clampCarouselIndex((int) Math.floor(localIndex + 0.5D));
        if (nearest != lastTickedMarkerIndex) {
            lastTickedMarkerIndex = nearest;
            playCarouselTickSound();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void playCarouselTickSound() {
        if (!CS2LootboxClientConfig.ENABLE_CAROUSEL_TICK_SOUND.get()) {
            return;
        }
        playUiSound(definition().carouselTickSound(), definition().soundProfile().carouselTickResolved());
    }

    @OnlyIn(Dist.CLIENT)
    private void playPrizeRevealSound() {
        if (prizeRevealSoundPlayed) {
            return;
        }
        prizeRevealSoundPlayed = true;
        playDefaultUiSound(determineRevealSoundId());
    }

    @OnlyIn(Dist.CLIENT)
    private void playPrizeAwardSound() {
        if (prizeAwardSoundPlayed) {
            return;
        }
        prizeAwardSoundPlayed = true;
        playUiSound(determineAwardSoundId(), determineAwardSoundTuning());
    }

    /** Sound played when the final carousel comes to rest on the item. */
    @OnlyIn(Dist.CLIENT)
    private @NotNull ResourceLocation determineRevealSoundId() {
        LootEntry entry = winningEntry();
        String tier = rarityTier(entry, clientWinningStack);
        return switch (tier) {
            // CS2 has no separate Common/Uncommon case reveal samples in the
            // bundled set, so lower custom tiers use the Rare reveal fallback.
            case "restricted" -> ModSounds.CASE_REVEAL_MYTHICAL.getId();
            case "classified" -> ModSounds.CASE_REVEAL_LEGENDARY.getId();
            case "covert", "special" -> ModSounds.CASE_REVEAL_ANCIENT.getId();
            default -> ModSounds.CASE_REVEAL_RARE.getId();
        };
    }

    /** Rarity-specific awarded sting played when the reward UI actually closes. */
    @OnlyIn(Dist.CLIENT)
    private @NotNull ResourceLocation determineAwardSoundId() {
        LootEntry entry = winningEntry();
        String tier = rarityTier(entry, clientWinningStack);
        return switch (tier) {
            case "industrial" -> definition().uncommonSound();
            case "milspec" -> definition().rareSound();
            case "restricted" -> definition().mythicalSound();
            case "classified" -> definition().classifiedSound();
            case "covert" -> definition().covertSound();
            case "special" -> definition().specialSound();
            default -> definition().rewardSound();
        };
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull LootboxDefinition.SoundTuning determineAwardSoundTuning() {
        LootEntry entry = winningEntry();
        String tier = rarityTier(entry, clientWinningStack);
        LootboxDefinition.SoundProfile profile = definition().soundProfile();
        return switch (tier) {
            case "industrial" -> profile.uncommonResolved();
            case "milspec" -> profile.rareResolved();
            case "restricted" -> profile.mythicalResolved();
            case "classified" -> profile.classifiedResolved();
            case "covert" -> profile.covertResolved();
            case "special" -> profile.specialResolved();
            default -> profile.rewardResolved();
        };
    }

    @OnlyIn(Dist.CLIENT)
    private void playDefaultUiSound(@Nullable ResourceLocation soundId) {
        playUiSound(soundId, definition().soundProfile().defaults());
    }

    @OnlyIn(Dist.CLIENT)
    private void playUiSound(@Nullable ResourceLocation soundId, @NotNull LootboxDefinition.SoundTuning tuning) {
        playUiSound(soundId, tuning.volume(), tuning.pitch());
    }

    @OnlyIn(Dist.CLIENT)
    private void playUiSound(@Nullable ResourceLocation soundId, float volume, float pitch) {
        if (!CS2LootboxClientConfig.ENABLE_UI_SOUNDS.get() || soundId == null) {
            return;
        }
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (sound == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static @NotNull String rarityTier(@Nullable LootEntry entry, @NotNull ItemStack stack) {
        if (entry != null && entry.rarityTier() != null && !entry.rarityTier().isBlank()) {
            return entry.rarityTier().toLowerCase(Locale.ROOT);
        }
        if (entry != null && entry.rarityTranslationKey() != null) {
            String lower = entry.rarityTranslationKey().toLowerCase(Locale.ROOT);
            if (lower.contains("special") || lower.contains("gold") || lower.contains("knife") || lower.contains("glove")) return "special";
            if (lower.contains("covert") || lower.contains("red")) return "covert";
            if (lower.contains("classified") || lower.contains("pink")) return "classified";
            if (lower.contains("restricted") || lower.contains("purple")) return "restricted";
            if (lower.contains("milspec") || lower.contains("blue")) return "milspec";
            if (lower.contains("industrial")) return "industrial";
        }
        Rarity rarity = stack.isEmpty() ? Rarity.COMMON : stack.getRarity();
        return switch (rarity) {
            case EPIC -> "classified";
            case RARE -> "restricted";
            case UNCOMMON -> "milspec";
            default -> "consumer";
        };
    }

    @OnlyIn(Dist.CLIENT)
    private void drawMarkerOverlay(@NotNull GuiGraphics graphics, int markerX, int markerY, int slotY, float alpha) {
        LootboxDefinition.UiSkin skin = definition().uiSkin();
        blitTinted(graphics, skin.markerBarTexture(), markerX - 5, slotY - 12, 10,
                CAROUSEL_SLOT_HEIGHT + 24, 0xF2D04B, alpha * 0.95F);
        blitTinted(graphics, skin.markerOuterCircleTexture(), markerX - 60, markerY - 60,
                120, 120, 0xFFFFFF, alpha * 0.92F);
        blitTinted(graphics, skin.markerInnerCircleTexture(), markerX - 52, markerY - 52,
                104, 104, 0xFFFFFF, alpha * 0.50F);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawTexturedSlotBorder(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int tint, float alpha) {
        blitTinted(graphics, definition().uiSkin().slotBorderTexture(), x, y, width, height, tint, alpha);
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull ResourceLocation resolveLegendaryForeground(@Nullable ResourceLocation requested) {
        ResourceLocation fallback = LegendaryLoot.DEFAULT_FOREGROUND;
        if (requested == null) {
            return fallback;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager().getResource(requested).isPresent() ? requested : fallback;
    }

    @OnlyIn(Dist.CLIENT)
    private void blitPlain(@NotNull GuiGraphics graphics, @Nullable ResourceLocation texture, int x, int y, int width, int height, float alpha) {
        blitTinted(graphics, texture, x, y, width, height, 0xFFFFFF, alpha);
    }

    @OnlyIn(Dist.CLIENT)
    private void blitTinted(@NotNull GuiGraphics graphics, @Nullable ResourceLocation texture, int x, int y, int width, int height, int rgb, float alpha) {
        if (texture == null || width <= 0 || height <= 0) {
            return;
        }
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, Math.max(0.0F, Math.min(1.0F, alpha)));
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawClippedPrizeGlow(
            @NotNull GuiGraphics graphics,
            ResourceLocation texture,
            int centerX,
            int centerY,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom,
            int tint,
            float alpha) {
        if (texture == null || alpha <= 0.0F || clipRight <= clipLeft || clipBottom <= clipTop) {
            return;
        }

        // Do NOT use a scissor here. Scissor rectangles are ultimately tied to
        // the current window/framebuffer state and can become stale/misaligned
        // across a live resize/maximize. Instead draw exactly one quad covering
        // the result panel and rotate the TEXTURE COORDINATES inside that quad.
        // The geometry can therefore never leave the card, regardless of the
        // current GUI scale or window size.
        long now = Util.getMillis();
        float rotation = (now % 120_000L) * (360.0F / 120_000.0F);

        float farthest = 0.0F;
        farthest = Math.max(farthest, distance(centerX, centerY, clipLeft, clipTop));
        farthest = Math.max(farthest, distance(centerX, centerY, clipRight, clipTop));
        farthest = Math.max(farthest, distance(centerX, centerY, clipRight, clipBottom));
        farthest = Math.max(farthest, distance(centerX, centerY, clipLeft, clipBottom));

        // A little overscan keeps all four panel corners inside the 0..1 UV
        // range while the radial texture rotates around the prize item.
        float textureSpan = Math.max(1.0F, farthest * 2.10F);

        drawPrizeGlowUvLayer(graphics, texture,
                clipLeft, clipTop, clipRight, clipBottom,
                centerX, centerY, textureSpan,
                rotation, tint, alpha * 0.58F, false);

        drawPrizeGlowUvLayer(graphics, texture,
                clipLeft, clipTop, clipRight, clipBottom,
                centerX, centerY, textureSpan,
                -rotation * 1.18F, tint, alpha * 0.42F, true);
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Draws the glow inside the fixed result-panel rectangle while rotating the
     * UVs around the item. This replaces framebuffer scissoring entirely, so a
     * window resize/maximize cannot offset the clipping region.
     */
    @OnlyIn(Dist.CLIENT)
    private static void drawPrizeGlowUvLayer(
            @NotNull GuiGraphics graphics,
            ResourceLocation texture,
            int left,
            int top,
            int right,
            int bottom,
            float centerX,
            float centerY,
            float textureSpan,
            float rotationDegrees,
            int tint,
            float alpha,
            boolean mirrored) {
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        if (alpha <= 0.0F) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, alpha);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float[] uvBL = rotatedGlowUv(left, bottom, centerX, centerY, textureSpan, rotationDegrees, mirrored);
        float[] uvBR = rotatedGlowUv(right, bottom, centerX, centerY, textureSpan, rotationDegrees, mirrored);
        float[] uvTR = rotatedGlowUv(right, top, centerX, centerY, textureSpan, rotationDegrees, mirrored);
        float[] uvTL = rotatedGlowUv(left, top, centerX, centerY, textureSpan, rotationDegrees, mirrored);

        builder.vertex(matrix, left, bottom, 0.0F).uv(uvBL[0], uvBL[1]).endVertex();
        builder.vertex(matrix, right, bottom, 0.0F).uv(uvBR[0], uvBR[1]).endVertex();
        builder.vertex(matrix, right, top, 0.0F).uv(uvTR[0], uvTR[1]).endVertex();
        builder.vertex(matrix, left, top, 0.0F).uv(uvTL[0], uvTL[1]).endVertex();

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static @NotNull float[] rotatedGlowUv(
            float x,
            float y,
            float centerX,
            float centerY,
            float textureSpan,
            float rotationDegrees,
            boolean mirrored) {
        float dx = x - centerX;
        float dy = y - centerY;

        // Inverse rotation of the sample position = visible texture rotation.
        double radians = Math.toRadians(rotationDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float tx = cos * dx + sin * dy;
        float ty = -sin * dx + cos * dy;

        if (mirrored) {
            tx = -tx;
        }

        float u = 0.5F + tx / textureSpan;
        float v = 0.5F + ty / textureSpan;
        return new float[]{u, v};
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawLine(@NotNull GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(x1 + (x2 - x1) * t);
            int y = Math.round(y1 + (y2 - y1) * t);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawHeader(@NotNull GuiGraphics graphics, float alpha) {
        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;
        int centerX = position.x + size.width / 2;

        drawCentered(graphics, font, Component.translatable("cs2lootbox.case_screen.unlock_container"),
                centerX, position.y + 16, withAlpha(0xFFFFFF, alpha));
        drawCentered(graphics, font,
                Component.translatable("cs2lootbox.case_screen.unlock_case", caseDisplayName()),
                centerX, position.y + 34, withAlpha(0xFFDCDCDC, alpha));
        drawCentered(graphics, font, Component.translatable("cs2lootbox.case_screen.single_open"),
                centerX, position.y + 52, withAlpha(0xFFC7C7C7, alpha));
    }

    @OnlyIn(Dist.CLIENT)
    private void drawFooterControls(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float alpha, boolean opening, boolean showClose) {
        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;
        int footerTop = footerTop();
        int footerBottom = position.y + size.height;
        int left = fullscreenReferenceLeft();
        int right = fullscreenReferenceRight();
        int fullWidth = Math.max(1, right - left);
        int separator1 = left + fullWidth / 3;
        int separator2 = left + (fullWidth * 2) / 3;

        graphics.fill(left, footerTop, right, footerBottom, withAlpha(0x090909, alpha * 0.56F));
        graphics.fill(left, footerTop, right, footerTop + 1, withAlpha(0x525252, alpha * 0.65F));
        graphics.fill(separator1, footerTop + 5, separator1 + 1, footerBottom - 5, withAlpha(0x4C4C4C, alpha * 0.65F));
        graphics.fill(separator2, footerTop + 5, separator2 + 1, footerBottom - 5, withAlpha(0x4C4C4C, alpha * 0.65F));

        // The key has already been consumed/validated by the time the roulette
        // is visible. Keep the carousel footer uncluttered instead of still
        // showing the pre-open "Use <key>" prompt.

        int openX = openButtonX();
        int openY = openButtonY();
        int openW = openButtonWidth();
        int openH = openButtonHeight();
        boolean openHover = !opening && mouseX >= openX && mouseX < openX + openW && mouseY >= openY && mouseY < openY + openH;
        int openFill = opening ? 0x2C2C2C : (openHover ? 0x424242 : 0x303030);
        graphics.fill(openX, openY, openX + openW, openY + openH, withAlpha(openFill, alpha * 0.95F));
        drawBorder(graphics, openX, openY, openW, openH, withAlpha(opening ? 0x787878 : 0xB4B4B4, alpha));
        Component openText = opening
                ? Component.translatable("cs2lootbox.case_screen.opening")
                : Component.translatable("cs2lootbox.case_screen.open_button");
        drawCentered(graphics, font, openText, openX + openW / 2, openY + 8, withAlpha(0xFFFFFF, alpha));
        if (opening) {
            drawCentered(graphics, font, Component.literal(spinnerFrame()), openX + 18, openY + 8, withAlpha(0xFFFFFF, alpha));
        }

        if (showClose) {
            int closeX = closeButtonX();
            int closeY = closeButtonY();
            int closeW = closeButtonWidth();
            int closeH = closeButtonHeight();
            boolean closeHover = mouseX >= closeX && mouseX < closeX + closeW && mouseY >= closeY && mouseY < closeY + closeH;
            int closeFill = closeHover ? 0x262626 : 0x181818;
            graphics.fill(closeX, closeY, closeX + closeW, closeY + closeH, withAlpha(closeFill, alpha * 0.95F));
            drawBorder(graphics, closeX, closeY, closeW, closeH, withAlpha(0x7A7A7A, alpha));
            drawCentered(graphics, font, Component.translatable("cs2lootbox.case_screen.close_button"),
                    closeX + closeW / 2, closeY + 8, withAlpha(0xFFFFFF, alpha));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component caseDisplayName() {
        if (definition().caseTranslationKey() != null && !definition().caseTranslationKey().isBlank() && I18n.exists(definition().caseTranslationKey())) {
            return Component.translatable(definition().caseTranslationKey());
        }
        Item item = ForgeRegistries.ITEMS.getValue(definition().caseItemId());
        if (item != null) {
            String hover = new ItemStack(item).getHoverName().getString();
            if (hover != null && !hover.isBlank() && !hover.contains(".")) {
                return Component.literal(hover);
            }
        }
        return Component.literal(prettifyId(definition().caseItemId().getPath()));
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component keyDisplayName() {
        if (definition().keyTranslationKey() != null && !definition().keyTranslationKey().isBlank() && I18n.exists(definition().keyTranslationKey())) {
            return Component.translatable(definition().keyTranslationKey());
        }
        Item item = ForgeRegistries.ITEMS.getValue(definition().keyItemId());
        if (item != null) {
            String hover = new ItemStack(item).getHoverName().getString();
            if (hover != null && !hover.isBlank() && !hover.contains(".")) {
                return Component.literal(hover);
            }
        }
        return Component.literal(prettifyId(definition().keyItemId().getPath()));
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull ItemStack keyDisplayStack() {
        Item item = ForgeRegistries.ITEMS.getValue(definition().keyItemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull String spinnerFrame() {
        String[] frames = {"|", "/", "-", "\\"};
        int index = (int) ((Util.getMillis() / 120L) % frames.length);
        return frames[index];
    }

    @OnlyIn(Dist.CLIENT)
    private void closeUi() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawBorder(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawCircleRing(@NotNull GuiGraphics graphics, int cx, int cy, int radius, int color, int thickness) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 260.0F);
        try {
            int points = Math.max(48, radius * 5);
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                int x = cx + (int) Math.round(Math.cos(angle) * radius);
                int y = cy + (int) Math.round(Math.sin(angle) * radius);
                graphics.fill(x - thickness / 2, y - thickness / 2,
                        x - thickness / 2 + thickness, y - thickness / 2 + thickness, color);
            }
        } finally {
            pose.popPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawFilledCircle(@NotNull GuiGraphics graphics, int cx, int cy, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
            graphics.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, color);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawCentered(@NotNull GuiGraphics graphics, @NotNull Font font, @NotNull Component component, int x, int y, int color) {
        graphics.drawString(font, component, x - font.width(component) / 2, y, color, true);
    }

    private static int packLight(int blockLight, int skyLight) {
        return (blockLight << 4) | (skyLight << 20);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    private float phaseProgress(long durationMs) {
        if (durationMs <= 0L) {
            return 1.0F;
        }
        long elapsed = Math.max(0L, Util.getMillis() - phaseStartedAtMs);
        return Math.min(1.0F, elapsed / (float) durationMs);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private int clampCarouselIndex(int index) {
        if (carouselSlots.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(carouselSlots.size() - 1, index));
    }

    private int footerTop() {
        return getPosition().y + getSize().height - 62;
    }

    private int openButtonWidth() {
        return 118;
    }

    private int openButtonHeight() {
        return 26;
    }

    private int openButtonX() {
        return getPosition().x + (getSize().width - openButtonWidth()) / 2;
    }

    private int openButtonY() {
        return footerTop() + 13;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isInsideOpenButton(double mouseX, double mouseY) {
        int x = openButtonX();
        int y = openButtonY();
        return mouseX >= x && mouseX < x + openButtonWidth()
                && mouseY >= y && mouseY < y + openButtonHeight();
    }

    private int closeButtonWidth() {
        return 94;
    }

    private int closeButtonHeight() {
        return 26;
    }

    private int closeButtonX() {
        return getPosition().x + getSize().width - closeButtonWidth() - 34;
    }

    private int closeButtonY() {
        return footerTop() + 13;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isInsideCloseButton(double mouseX, double mouseY) {
        int x = closeButtonX();
        int y = closeButtonY();
        return mouseX >= x && mouseX < x + closeButtonWidth()
                && mouseY >= y && mouseY < y + closeButtonHeight();
    }

    /**
     * The runtime UI is authored at 960x540 and uniformly scaled by ScaledWidgetGroup.
     * On displays wider than 16:9 this leaves small physical side gutters. Full-width
     * CS2 bands/backdrops should bleed through those gutters without stretching the
     * model, cards, circles or other authored content.
     */
    @OnlyIn(Dist.CLIENT)
    private int fullscreenHorizontalOverscan() {
        Size size = getSize();
        if (size.width <= 0 || size.height <= 0) {
            return 0;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return 0;
        }

        float scale = Math.min(screenWidth / (float) size.width, screenHeight / (float) size.height);
        if (scale <= 0.0F) {
            return 0;
        }

        float sideGutterPixels = Math.max(0.0F, (screenWidth - size.width * scale) * 0.5F);
        return (int) Math.ceil(sideGutterPixels / scale);
    }

    @OnlyIn(Dist.CLIENT)
    private int fullscreenReferenceLeft() {
        return getPosition().x - fullscreenHorizontalOverscan();
    }

    @OnlyIn(Dist.CLIENT)
    private int fullscreenReferenceRight() {
        return getPosition().x + getSize().width + fullscreenHorizontalOverscan();
    }

    private int prizePanelWidth() {
        return Math.min(560, getSize().width - 120);
    }

    private int prizePanelHeight() {
        return Math.min(250, getSize().height - 160);
    }

    private int prizePanelLeft() {
        return getPosition().x + (getSize().width - prizePanelWidth()) / 2;
    }

    private int prizePanelTop() {
        return getPosition().y + (getSize().height - prizePanelHeight()) / 2 - 10;
    }

    private int prizePanelRight() {
        return prizePanelLeft() + prizePanelWidth();
    }

    private int prizePanelBottom() {
        return prizePanelTop() + prizePanelHeight();
    }

    private int inventoryButtonWidth() {
        return 138;
    }

    private int closePrizeButtonWidth() {
        return 78;
    }

    private int prizeButtonHeight() {
        return 24;
    }

    private int prizeButtonsY() {
        return prizePanelBottom() - prizeButtonHeight() - 10;
    }

    private int inventoryButtonX() {
        return prizePanelRight() - closePrizeButtonWidth() - 10 - inventoryButtonWidth() - 20;
    }

    private int closePrizeButtonX() {
        return prizePanelRight() - closePrizeButtonWidth() - 20;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isInsideInventoryButton(double mouseX, double mouseY) {
        int x = inventoryButtonX();
        int y = prizeButtonsY();
        return mouseX >= x && mouseX < x + inventoryButtonWidth()
                && mouseY >= y && mouseY < y + prizeButtonHeight();
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isInsideClosePrizeButton(double mouseX, double mouseY) {
        int x = closePrizeButtonX();
        int y = prizeButtonsY();
        return mouseX >= x && mouseX < x + closePrizeButtonWidth()
                && mouseY >= y && mouseY < y + prizeButtonHeight();
    }

    private static @NotNull String prettifyId(@NotNull String path) {
        String[] parts = path.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }

    private enum ClientPhase {
        CASE,
        CASE_LOADING,
        ROLL,
        SNAP,
        PRIZE,
        CLOSING
    }

    private enum CarouselStage {
        PRIMARY,
        LEGENDARY
    }

    private enum PrizeAction {
        VIEW_INVENTORY,
        CLOSE
    }

    private record CarouselSlot(@Nullable LootEntry entry, @NotNull ItemStack stack, int legendaryIndex) {
        private static @NotNull CarouselSlot empty() {
            return new CarouselSlot(null, ItemStack.EMPTY, -1);
        }

        private static @NotNull CarouselSlot item(@NotNull LootEntry entry, @Nullable ItemStack stack) {
            return new CarouselSlot(entry, stack == null ? ItemStack.EMPTY : stack, -1);
        }

        private static @NotNull CarouselSlot legendary(int legendaryIndex) {
            return new CarouselSlot(null, ItemStack.EMPTY, legendaryIndex);
        }
    }
}
