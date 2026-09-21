package net.tokyosu.cs2lootbox.client.widget;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;
import net.tokyosu.cs2lootbox.api.lootbox.LegendaryLoot;
import net.tokyosu.cs2lootbox.api.lootbox.LootEntry;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.config.CS2LootboxClientConfig;
import net.tokyosu.cs2lootbox.loot.LootboxLootRoller;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Pure 2D chrome for the opening screen.
 *
 * Kept as a separate sibling of LootboxModelWidget so GeckoLib's custom 3D
 * render pass cannot overwrite the shader/depth state used by the controls.
 */
public final class LootboxOverlayWidget extends Widget {
    private static final int FOOTER_HEIGHT = 62;
    private static final int BUTTON_HEIGHT = 32;
    private static final int OPEN_WIDTH = 146;
    private static final int CLOSE_WIDTH = 78;
    private static final int BUTTON_GAP = 10;

    private static final int LIST_HEIGHT = 184;
    private static final int SLOT_WIDTH = 72;
    private static final int SLOT_HEIGHT = 66;
    private static final int SLOT_MIN_GAP = 5;
    private static final int SLOT_ROW_GAP = 11;
    private static final int MAX_COLUMNS = 11;
    private static final ResourceLocation LEGENDARY_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CS2LootBoxMod.MOD_ID, "textures/gui/glow_radial.png");
    private static final int LEGENDARY_GOLD = 0xFFD85A;
    private static final int LEGENDARY_TOP = 0x4A3A0D;
    private static final int LEGENDARY_CENTER = 0xA77D17;
    private static final int LEGENDARY_BOTTOM = 0x57400C;

    private final LootboxModelWidget model;

    // Only regular crate.loot(...) entries can be inspected. Legendary panels
    // are category/background entries, not concrete items, so they are never
    // assigned here.
    private @Nullable LootEntry inspectedEntry;
    private @NotNull ItemStack inspectedStack = ItemStack.EMPTY;

    // 3D inspection camera. This is intentionally independent from the
    // item's own model/display transforms so future guns/skins/knives can use
    // their normal ItemRenderer / GeckoLib / custom renderer unchanged.
    private float inspectYaw = -28.0F;
    private float inspectPitch = 12.0F;
    private float inspectZoom = 1.0F;


    public LootboxOverlayWidget(int x, int y, int width, int height, @NotNull LootboxModelWidget model) {
        super(x, y, width, height);
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);

        if (model.shouldDrawOpeningChrome()) {
            return;
        }

        if (!CS2LootboxClientConfig.ENABLE_3D_ITEM_INSPECTION.get()
                && inspectedEntry != null) {
            closeInspector();
        }

        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 500.0F);
        try {
            boolean inspecting = inspectedEntry != null && !inspectedStack.isEmpty();

            if (inspecting) {
                /*
                 * The inspector is modal, so do not render item_list/footer
                 * ItemStacks behind it at all. Enchanted/foil items can use
                 * dedicated fixed RenderTypes (and custom item renderers may
                 * own additional buffers); merely painting an opaque panel on
                 * top is therefore not a reliable occlusion boundary.
                 *
                 * Finish anything left from the previous frame, then render
                 * only the modal inspector for this frame. This guarantees an
                 * item from item_list cannot be emitted later above the modal.
                 */
                flushVanillaItemBuffers(graphics);
                drawItemInspector(graphics, mouseX, mouseY);
                flushVanillaItemBuffers(graphics);
            } else {
                drawHeader(graphics);
                drawLootPanel(graphics, mouseX, mouseY);

                // Finish every item_list pass before drawing later chrome.
                flushVanillaItemBuffers(graphics);

                drawFooter(graphics, mouseX, mouseY);
                drawStatus(graphics);
                flushVanillaItemBuffers(graphics);
            }

            graphics.flush();
        } finally {
            pose.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (model.shouldDrawOpeningChrome()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // While an item is being inspected, keep the detail view modal so a
        // click cannot accidentally press OPEN/CLOSE underneath it.
        if (inspectedEntry != null) {
            if (button == 1 || (button == 0 && insideInspectBack(mouseX, mouseY))) {
                closeInspector();
            }
            return true;
        }

        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Item inspection is only available in the idle case screen and can
        // be disabled entirely from the Forge client config.
        if (CS2LootboxClientConfig.ENABLE_3D_ITEM_INSPECTION.get()
                && model.canStartOpeningFromChrome()) {
            LootEntry inspect = findInspectableLootAt(mouseX, mouseY);
            if (inspect != null) {
                openInspector(inspect);
                return true;
            }
        }

        if (model.canStartOpeningFromChrome() && insideOpen(mouseX, mouseY)) {
            model.startOpeningFromChrome();
            return true;
        }

        if (model.canCloseFromChrome() && insideClose(mouseX, mouseY)) {
            model.closeFromChrome();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (CS2LootboxClientConfig.ENABLE_3D_ITEM_INSPECTION.get()
                && inspectedEntry != null && button == 0) {
            inspectYaw += (float) dragX * 0.85F;

            // GUI mouse Y grows downward, while positive X-axis rotation makes
            // the inspected model tilt the opposite way. Invert dragY so
            // dragging upward/downward follows the mouse naturally.
            inspectPitch -= (float) dragY * 0.85F;
            inspectPitch = Math.max(-85.0F, Math.min(85.0F, inspectPitch));
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (CS2LootboxClientConfig.ENABLE_3D_ITEM_INSPECTION.get()
                && inspectedEntry != null) {
            inspectZoom += (float) wheelDelta * 0.10F;
            inspectZoom = Math.max(0.45F, Math.min(2.25F, inspectZoom));
            return true;
        }

        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawHeader(@NotNull GuiGraphics graphics) {
        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;
        int centerX = position.x + size.width / 2;

        drawCentered(graphics, font,
                Component.translatable("cs2lootbox.case_screen.unlock_container"),
                centerX, position.y + 16, 0xFFF4F4F4);
        drawCentered(graphics, font,
                Component.translatable("cs2lootbox.case_screen.unlock_case", caseName()),
                centerX, position.y + 34, 0xFFDCDCDC);
        drawCentered(graphics, font,
                Component.translatable("cs2lootbox.case_screen.single_open"),
                centerX, position.y + 52, 0xFFC7C7C7);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawLootPanel(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;
        LootboxDefinition definition = model.getDefinition();
        List<LootEntry> loot = definition.loot();
        List<LegendaryLoot> legendaryLoot = definition.legendaryLoot();
        int totalEntries = loot.size() + legendaryLoot.size();
        if (totalEntries == 0) {
            return;
        }

        int left = fullscreenReferenceLeft();
        int right = fullscreenReferenceRight();
        int top = footerTop() - LIST_HEIGHT - 10;
        int bottom = footerTop() - 8;
        int titleY = top + 7;

        graphics.fill(left, top, right, bottom, 0x30645F5A);
        graphics.fill(left, top, right, bottom, 0x18312F2D);
        graphics.fill(left, top, right, top + 1, 0x5CFFFFFF);
        graphics.fill(left, bottom - 1, right, bottom, 0x22000000);

        Component title = Component.translatable("cs2lootbox.case_screen.receive_one_following");
        drawCentered(graphics, font, title, position.x + size.width / 2, titleY, 0xFFE6E6E6);

        if (CS2LootboxClientConfig.ENABLE_3D_ITEM_INSPECTION.get()) {
            Component inspect = Component.translatable("cs2lootbox.case_screen.inspect_items");
            graphics.drawString(font, inspect, right - font.width(inspect) - 10, titleY, 0xFFEAEAEA, false);
        }

        int usableLeft = position.x + 8;
        int usableRight = position.x + size.width - 8;
        int usableTop = top + 24;
        int usableBottom = bottom - 10;

        int rows = totalEntries > MAX_COLUMNS ? 2 : 1;
        int columns = Math.min(MAX_COLUMNS, Math.max(1, (int) Math.ceil(totalEntries / (double) rows)));
        int horizontalGap = 10;
        int contentWidth = columns * SLOT_WIDTH + Math.max(0, columns - 1) * horizontalGap;
        int startX = usableLeft + Math.max(0, (usableRight - usableLeft - contentWidth) / 2);

        for (int i = 0; i < totalEntries; i++) {
            int row = i / columns;
            int col = i % columns;
            int x = startX + col * (SLOT_WIDTH + horizontalGap);
            int y = usableTop + row * (SLOT_HEIGHT + SLOT_ROW_GAP);
            if (y + SLOT_HEIGHT > usableBottom) {
                break;
            }

            if (i < loot.size()) {
                boolean hovered = CS2LootboxClientConfig.ENABLE_3D_ITEM_INSPECTION.get()
                        && model.canStartOpeningFromChrome()
                        && inside(mouseX, mouseY, x, y, SLOT_WIDTH, SLOT_HEIGHT);
                drawLootEntry(graphics, font, loot.get(i), x, y, SLOT_WIDTH, SLOT_HEIGHT, hovered);
            } else {
                // Legendary panels intentionally have no hover/click inspect
                // behavior because legendary() represents a category/table,
                // not one concrete item.
                drawLegendaryEntry(graphics, font, legendaryLoot.get(i - loot.size()), x, y, SLOT_WIDTH, SLOT_HEIGHT);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawLootEntry(@NotNull GuiGraphics graphics, @NotNull Font font, @NotNull LootEntry entry, int x, int y, int width, int height, boolean hovered) {
        ItemStack stack = LootboxLootRoller.createPreviewStack(entry);
        int rarity = rarityColor(entry, stack);

        graphics.fill(x, y, x + width, y + height, hovered ? 0x665F6469 : 0x465B5957);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? 0x48484B4F : 0x303B3A39);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 18, hovered ? 0x28FFFFFF : 0x18FFFFFF);
        drawBorder(graphics, x, y, width, height, hovered ? 0xA8FFFFFF : 0x26FFFFFF);
        graphics.fill(x + 1, y + height - 4, x + width - 1, y + height - 1, (0xB8000000 | rarity));
        blitTinted(graphics, model.getDefinition().uiSkin().slotBorderTexture(), x, y, width, height, rarity, 0.28F);

        if (!stack.isEmpty()) {
            renderItemScaled(
                    graphics,
                    stack,
                    x + width / 2,
                    y + 20,
                    1.58F,
                    0.78F,
                    entry.itemListTransform()
            );
            if (stack.getCount() > 1) {
                String count = Integer.toString(stack.getCount());
                graphics.drawString(font, count, x + width - 4 - font.width(count), y + 4, 0xFFFFFFFF, true);
            }
        }

        String sourceFallback = (entry.itemTagSource() ? "#" : "") + entry.itemId();
        String name = entry.nameTranslationKey() != null
                ? Component.translatable(entry.nameTranslationKey()).getString()
                : (stack.isEmpty() ? sourceFallback : stack.getHoverName().getString());
        name = ellipsize(font, name, width - 8);
        graphics.drawString(font, name, x + 4, y + height - 12, 0xFFF8F8F8, false);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawLegendaryEntry(
            @NotNull GuiGraphics graphics,
            @NotNull Font font,
            @NotNull LegendaryLoot legendary,
            int x,
            int y,
            int width,
            int height) {
        int middleY = y + height / 2;
        graphics.fillGradient(x, y, x + width, middleY, 0xFF4A3A0D, 0xFFA77D17);
        graphics.fillGradient(x, middleY, x + width, y + height, 0xFFA77D17, 0xFF57400C);

        blitTinted(graphics, LEGENDARY_GLOW_TEXTURE,
                x + 7, y + 2, width - 14, height - 8,
                LEGENDARY_GOLD, 0.58F);

        ResourceLocation foreground = resolveLegendaryForeground(legendary.foreground());
        int iconWidth = Math.min(width - 8, 62);
        int iconHeight = Math.max(1, Math.round(iconWidth * (198.0F / 256.0F)));
        int iconY = y + 3;
        blitTinted(graphics, foreground,
                x + (width - iconWidth) / 2,
                iconY,
                iconWidth,
                iconHeight,
                0xFFFFFF,
                1.0F);

        drawBorder(graphics, x, y, width, height, 0x66FFE789);
        graphics.fill(x + 1, y + height - 4, x + width - 1, y + height - 1, 0xE0FFD700);
        blitTinted(graphics, model.getDefinition().uiSkin().slotBorderTexture(),
                x, y, width, height, LEGENDARY_GOLD, 0.34F);

        String name = Component.translatable("cs2lootbox.legendary.special_item").getString();
        name = ellipsize(font, name, width - 8);
        graphics.drawString(font, name, x + 4, y + height - 12, 0xFFFFFFFF, false);
    }

    @OnlyIn(Dist.CLIENT)
    private static @NotNull ResourceLocation resolveLegendaryForeground(@Nullable ResourceLocation requested) {
        ResourceLocation fallback = LegendaryLoot.DEFAULT_FOREGROUND;
        if (requested == null) {
            return fallback;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager().getResource(requested).isPresent() ? requested : fallback;
    }

    @OnlyIn(Dist.CLIENT)
    private void openInspector(@NotNull LootEntry entry) {
        ItemStack previewStack = LootboxLootRoller.createPreviewStack(entry);
        if (previewStack.isEmpty()) {
            return;
        }

        inspectedEntry = entry;
        inspectedStack = previewStack;
        inspectYaw = -28.0F;
        inspectPitch = 12.0F;
        inspectZoom = 1.0F;
    }

    @OnlyIn(Dist.CLIENT)
    private void closeInspector() {
        inspectedEntry = null;
        inspectedStack = ItemStack.EMPTY;
        inspectYaw = -28.0F;
        inspectPitch = 12.0F;
        inspectZoom = 1.0F;
    }

    /**
     * Returns a normal crate.loot(...) entry at the clicked point.
     * legendary() panels are deliberately not considered.
     */
    @OnlyIn(Dist.CLIENT)
    private @Nullable LootEntry findInspectableLootAt(double mouseX, double mouseY) {
        LootboxDefinition definition = model.getDefinition();
        List<LootEntry> loot = definition.loot();
        int totalEntries = loot.size() + definition.legendaryLoot().size();
        if (loot.isEmpty() || totalEntries == 0) {
            return null;
        }

        Position position = getPosition();
        Size size = getSize();
        int top = footerTop() - LIST_HEIGHT - 10;
        int bottom = footerTop() - 8;
        int usableLeft = position.x + 8;
        int usableRight = position.x + size.width - 8;
        int usableTop = top + 24;
        int usableBottom = bottom - 10;

        int rows = totalEntries > MAX_COLUMNS ? 2 : 1;
        int columns = Math.min(MAX_COLUMNS, Math.max(1, (int) Math.ceil(totalEntries / (double) rows)));
        int horizontalGap = 10;
        int contentWidth = columns * SLOT_WIDTH + Math.max(0, columns - 1) * horizontalGap;
        int startX = usableLeft + Math.max(0, (usableRight - usableLeft - contentWidth) / 2);

        for (int i = 0; i < loot.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int x = startX + col * (SLOT_WIDTH + horizontalGap);
            int y = usableTop + row * (SLOT_HEIGHT + SLOT_ROW_GAP);
            if (y + SLOT_HEIGHT > usableBottom) {
                break;
            }

            if (inside(mouseX, mouseY, x, y, SLOT_WIDTH, SLOT_HEIGHT)) {
                return loot.get(i);
            }
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    private void drawItemInspector(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        LootEntry entry = inspectedEntry;
        if (entry == null || inspectedStack.isEmpty()) {
            return;
        }

        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;

        int panelWidth = Math.min(720, Math.max(430, size.width - 38));
        int panelHeight = Math.min(360, Math.max(260, size.height - 64));
        int panelX = position.x + (size.width - panelWidth) / 2;
        int panelY = position.y + Math.max(18, (size.height - panelHeight) / 2 - 4);
        int panelRight = panelX + panelWidth;
        int panelBottom = panelY + panelHeight;
        int rarity = rarityColor(entry, inspectedStack);

        graphics.fill(fullscreenReferenceLeft(), position.y,
                fullscreenReferenceRight(), position.y + size.height,
                0xB8000000);
        // Modal content must fully occlude item_list render passes below it.
        graphics.fill(panelX, panelY, panelRight, panelBottom, 0xFF0D1016);
        graphics.fill(panelX, panelY, panelX + 5, panelBottom, 0xFF000000 | rarity);
        graphics.fill(panelX + 5, panelY, panelRight, panelY + 1, 0x50FFFFFF);
        graphics.fill(panelX + 5, panelBottom - 1, panelRight, panelBottom, 0x32000000);

        Component title = Component.translatable("cs2lootbox.case_screen.inspect_title");
        graphics.drawString(font, title, panelX + 18, panelY + 14, 0xFFF4F4F4, true);

        int backWidth = 58;
        int backHeight = 22;
        int backX = panelRight - backWidth - 12;
        int backY = panelY + 9;
        boolean backHover = inside(mouseX, mouseY, backX, backY, backWidth, backHeight);
        graphics.fill(backX, backY, backX + backWidth, backY + backHeight,
                backHover ? 0xFF464646 : 0xFF2A2A2A);
        drawBorder(graphics, backX, backY, backWidth, backHeight, 0x62FFFFFF);
        drawCentered(graphics, font,
                Component.translatable("cs2lootbox.case_screen.inspect_back"),
                backX + backWidth / 2, backY + 7, 0xFFFFFFFF);

        // Left side = actual 3D item viewport.
        int previewLeft = panelX + 14;
        int previewTop = panelY + 42;
        int previewWidth = Math.max(220, (int) (panelWidth * 0.62F));
        int previewRight = Math.min(panelRight - 160, previewLeft + previewWidth);
        int previewBottom = panelBottom - 18;
        int previewCenterX = (previewLeft + previewRight) / 2;
        int previewCenterY = (previewTop + previewBottom) / 2;

        graphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0x3A05070B);
        graphics.fill(previewLeft, previewTop, previewRight, previewTop + 1, 0x28FFFFFF);
        graphics.fill(previewLeft, previewBottom - 1, previewRight, previewBottom, 0x24000000);

        LootboxDefinition.PreviewTransform previewLighting = model.getDefinition().preview();

        /*
         * Keep every inspected-item pass, including foil/glint, strictly inside
         * the 3D viewport. renderInspectableItem3D flushes its buffers before
         * this scissor is released.
         */
        graphics.enableScissor(previewLeft, previewTop, previewRight, previewBottom);
        try {
            renderInspectableItem3D(graphics, inspectedStack,
                    previewCenterX, previewCenterY,
                    inspectYaw, inspectPitch, inspectZoom,
                    packLight(previewLighting.blockLight(), previewLighting.skyLight()));
        } finally {
            graphics.disableScissor();
        }

        Component hint = Component.translatable("cs2lootbox.case_screen.inspect_controls");
        drawCentered(graphics, font, hint,
                previewCenterX, previewBottom - 15, 0xFFAEB4C0);

        // Right side = item metadata. The 3D viewport remains the primary focus.
        int textX = previewRight + 18;
        int textRight = panelRight - 18;
        int textWidth = Math.max(70, textRight - textX);
        int textY = panelY + 58;

        Component itemName = entry.nameTranslationKey() != null
                ? Component.translatable(entry.nameTranslationKey())
                : inspectedStack.getHoverName();
        graphics.drawString(font, itemName, textX, textY, 0xFFFFFFFF, true);
        textY += 18;

        Component rarityName = rarityName(entry);
        if (rarityName != null) {
            graphics.drawString(font, rarityName, textX, textY,
                    0xFF000000 | rarity, false);
            textY += 18;
        }

        if (entry.minCount() != entry.maxCount() || entry.minCount() > 1) {
            Component quantity = entry.minCount() == entry.maxCount()
                    ? Component.translatable("cs2lootbox.case_screen.inspect_quantity_single", entry.minCount())
                    : Component.translatable("cs2lootbox.case_screen.inspect_quantity_range", entry.minCount(), entry.maxCount());
            graphics.drawString(font, quantity, textX, textY, 0xFFBFC4CE, false);
            textY += 16;
        }

        for (String descriptionKey : entry.descriptionTranslationKeys()) {
            if (textY > panelBottom - 22) {
                break;
            }
            String line = Component.translatable(descriptionKey).getString();
            line = ellipsize(font, line, textWidth);
            graphics.drawString(font, line, textX, textY, 0xFFC8CCD4, false);
            textY += 13;
        }
    }

    /**
     * True 3D item inspection.
     *
     * Minecraft's own shared RenderBuffers are used so foil/glint and custom
     * item renderers behave exactly like normal Minecraft rendering.
     *
     * For the base/non-glint passes we keep the previous stable-lighting fix:
     * submitted normals are replaced by a constant upward normal. This prevents
     * the inspected item from becoming brighter/darker as it is rotated.
     *
     * Glint RenderTypes are NEVER wrapped, so enchanted/NBT-driven foil keeps
     * Minecraft's original rendering path.
     */
    @OnlyIn(Dist.CLIENT)
    private void renderInspectableItem3D(
            @NotNull GuiGraphics graphics,
            @NotNull ItemStack stack,
            int centerX,
            int centerY,
            float yaw,
            float pitch,
            float zoom,
            int packedLight) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = graphics.pose();

        // Absolutely finish any item_list foil pass before entering the modal
        // item's isolated depth pass.
        flushVanillaItemBuffers(graphics);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clearDepth(1.0D);
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        pose.pushPose();
        try {
            pose.translate(centerX, centerY, 820.0F);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));

            float renderScale = 92.0F * zoom;
            pose.scale(renderScale, -renderScale, renderScale);

            MultiBufferSource.BufferSource vanillaBuffers =
                    minecraft.renderBuffers().bufferSource();

            MultiBufferSource stableLightingBuffers = renderType -> {
                VertexConsumer consumer = vanillaBuffers.getBuffer(renderType);
                return isGlintRenderType(renderType)
                        ? consumer
                        : new ConstantNormalVertexConsumer(consumer);
            };

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            minecraft.getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    pose,
                    stableLightingBuffers,
                    minecraft.level,
                    0
            );

            // Flush while the inspector pose/depth state is still active.
            vanillaBuffers.endBatch();
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

    /**
     * Force Minecraft's shared item buffers to finish now.
     *
     * The foil/glint RenderTypes use dedicated fixed buffers. If they survive
     * until later in the LDLib draw pass they can be rendered above panels that
     * were actually drawn after the item.
     */
    @OnlyIn(Dist.CLIENT)
    private static void flushVanillaItemBuffers(@NotNull GuiGraphics graphics) {
        graphics.flush();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    private static boolean isGlintRenderType(@NotNull RenderType renderType) {
        return renderType == RenderType.glint()
                || renderType == RenderType.glintDirect()
                || renderType == RenderType.glintTranslucent()
                || renderType == RenderType.entityGlint()
                || renderType == RenderType.entityGlintDirect()
                || renderType == RenderType.armorGlint()
                || renderType == RenderType.armorEntityGlint();
    }

    /**
     * Stable-lighting adapter used only for the inspected model's non-glint
     * passes. It preserves the requested texture/RenderType and only replaces
     * normals, so custom/GeckoLib item renderers remain supported.
     */
    private static final class ConstantNormalVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        private ConstantNormalVertexConsumer(@NotNull VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public @NotNull VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public @NotNull VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public @NotNull VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public @NotNull VertexConsumer normal(float x, float y, float z) {
            delegate.normal(0.0F, 1.0F, 0.0F);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }

    private static int packLight(int blockLight, int skyLight) {
        return (blockLight << 4) | (skyLight << 20);
    }

    @OnlyIn(Dist.CLIENT)
    private @Nullable Component rarityName(@NotNull LootEntry entry) {
        if (entry.rarityTranslationKey() != null && !entry.rarityTranslationKey().isBlank()) {
            return Component.translatable(entry.rarityTranslationKey());
        }

        if (entry.rarityTier() != null && !entry.rarityTier().isBlank()) {
            return Component.literal(prettifyId(entry.rarityTier()));
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean insideInspectBack(double mouseX, double mouseY) {
        if (inspectedEntry == null) {
            return false;
        }

        Position position = getPosition();
        Size size = getSize();
        int panelWidth = Math.min(720, Math.max(430, size.width - 38));
        int panelHeight = Math.min(360, Math.max(260, size.height - 64));
        int panelX = position.x + (size.width - panelWidth) / 2;
        int panelY = position.y + Math.max(18, (size.height - panelHeight) / 2 - 4);
        int backWidth = 58;
        int backHeight = 22;
        int backX = panelX + panelWidth - backWidth - 12;
        int backY = panelY + 9;
        return inside(mouseX, mouseY, backX, backY, backWidth, backHeight);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    @OnlyIn(Dist.CLIENT)
    private void drawFooter(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        Position position = getPosition();
        Size size = getSize();
        Font font = Minecraft.getInstance().font;

        int left = fullscreenReferenceLeft();
        int right = fullscreenReferenceRight();
        int top = footerTop();
        int bottom = position.y + size.height;

        graphics.fill(left, top, right, bottom, 0x7E111111);
        graphics.fill(left, top, right, top + 1, 0x42FFFFFF);

        int keyAreaLeft = position.x + 12;
        int keyAreaRight = position.x + size.width - (OPEN_WIDTH + CLOSE_WIDTH + BUTTON_GAP + 16);
        int centerY = top + (bottom - top) / 2;

        ItemStack keyStack = keyStack();
        if (!keyStack.isEmpty()) {
            graphics.renderItem(keyStack, keyAreaLeft, centerY - 8);
            graphics.flush();
            RenderSystem.disableDepthTest();
        }
        graphics.drawString(font,
                Component.translatable("cs2lootbox.case_screen.use_key", keyName()),
                keyAreaLeft + 24, centerY - 4, 0xFFDADADA, true);

        boolean opening = model.isOpeningFromChrome();
        int oy = top + 9;
        int ox = position.x + size.width - 12 - CLOSE_WIDTH - BUTTON_GAP - OPEN_WIDTH;
        int cx = position.x + size.width - 12 - CLOSE_WIDTH;

        boolean openHover = !opening && model.canStartOpeningFromChrome()
                && mouseX >= ox && mouseX < ox + OPEN_WIDTH
                && mouseY >= oy && mouseY < oy + BUTTON_HEIGHT;
        int openFill = opening ? 0xFF5B5B5B : (openHover ? 0xFF717171 : 0xFF636363);
        graphics.fill(ox, oy, ox + OPEN_WIDTH, oy + BUTTON_HEIGHT, openFill);
        graphics.fill(ox + 1, oy + 1, ox + OPEN_WIDTH - 1, oy + BUTTON_HEIGHT / 2, 0x18FFFFFF);
        drawBorder(graphics, ox, oy, OPEN_WIDTH, BUTTON_HEIGHT, 0x8AFFFFFF);

        Component openTitle = opening
                ? Component.translatable("cs2lootbox.case_screen.opening")
                : Component.translatable("cs2lootbox.case_screen.open_button");
        drawCentered(graphics, font, openTitle, ox + OPEN_WIDTH / 2, oy + 5, 0xFFFFFFFF);
        Component openSubtitle = opening
                ? Component.literal(spinnerFrame())
                : Component.translatable("cs2lootbox.case_screen.open_subtitle");
        drawCentered(graphics, font, openSubtitle, ox + OPEN_WIDTH / 2, oy + 18, 0xFFD2D2D2);

        if (model.canCloseFromChrome()) {
            boolean closeHover = mouseX >= cx && mouseX < cx + CLOSE_WIDTH
                    && mouseY >= oy && mouseY < oy + BUTTON_HEIGHT;
            graphics.fill(cx, oy, cx + CLOSE_WIDTH, oy + BUTTON_HEIGHT,
                    closeHover ? 0xFF323232 : 0xFF232323);
            graphics.fill(cx + 1, oy + 1, cx + CLOSE_WIDTH - 1, oy + BUTTON_HEIGHT / 2, 0x10FFFFFF);
            drawBorder(graphics, cx, oy, CLOSE_WIDTH, BUTTON_HEIGHT, 0x52FFFFFF);
            drawCentered(graphics, font,
                    Component.translatable("cs2lootbox.case_screen.close_button"),
                    cx + CLOSE_WIDTH / 2, oy + 11, 0xFFFFFFFF);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawStatus(@NotNull GuiGraphics graphics) {
        Component status = model.chromeStatus();
        if (status == null || model.canStartOpeningFromChrome()) {
            return;
        }
        Position position = getPosition();
        Size size = getSize();
        int color = model.hasChromeError() ? 0xFFFF6666 : 0xFFDCDCDC;
        drawCentered(graphics, Minecraft.getInstance().font, status,
                position.x + size.width / 2, footerTop() - 20, color);
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component caseName() {
        LootboxDefinition definition = model.getDefinition();
        if (definition.caseTranslationKey() != null && !definition.caseTranslationKey().isBlank() && I18n.exists(definition.caseTranslationKey())) {
            return Component.translatable(definition.caseTranslationKey());
        }
        Item item = ForgeRegistries.ITEMS.getValue(definition.caseItemId());
        if (item != null) {
            String hover = new ItemStack(item).getHoverName().getString();
            if (hover != null && !hover.isBlank() && !hover.contains(".")) {
                return Component.literal(hover);
            }
        }
        return Component.literal(prettifyId(definition.caseItemId().getPath()));
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull Component keyName() {
        LootboxDefinition definition = model.getDefinition();
        if (definition.keyTranslationKey() != null && !definition.keyTranslationKey().isBlank() && I18n.exists(definition.keyTranslationKey())) {
            return Component.translatable(definition.keyTranslationKey());
        }
        Item item = ForgeRegistries.ITEMS.getValue(definition.keyItemId());
        if (item != null) {
            String hover = new ItemStack(item).getHoverName().getString();
            if (hover != null && !hover.isBlank() && !hover.contains(".")) {
                return Component.literal(hover);
            }
        }
        return Component.literal(prettifyId(definition.keyItemId().getPath()));
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull ItemStack keyStack() {
        Item item = ForgeRegistries.ITEMS.getValue(model.getDefinition().keyItemId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

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

    private int footerTop() {
        return getPosition().y + getSize().height - FOOTER_HEIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean insideOpen(double mouseX, double mouseY) {
        Position position = getPosition();
        Size size = getSize();
        int right = position.x + size.width;
        int ox = right - 12 - CLOSE_WIDTH - BUTTON_GAP - OPEN_WIDTH;
        int oy = footerTop() + 9;
        return mouseX >= ox && mouseX < ox + OPEN_WIDTH && mouseY >= oy && mouseY < oy + BUTTON_HEIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean insideClose(double mouseX, double mouseY) {
        Position position = getPosition();
        Size size = getSize();
        int cx = position.x + size.width - 12 - CLOSE_WIDTH;
        int oy = footerTop() + 9;
        return mouseX >= cx && mouseX < cx + CLOSE_WIDTH && mouseY >= oy && mouseY < oy + BUTTON_HEIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawBorder(@NotNull GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawCentered(@NotNull GuiGraphics graphics, @NotNull Font font, @NotNull Component text, int x, int y, int color) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, true);
    }

    @OnlyIn(Dist.CLIENT)
    private static @NotNull String spinnerFrame() {
        String[] frames = {"|", "/", "-", "\\"};
        return frames[(int) ((Util.getMillis() / 120L) % frames.length)];
    }

    @OnlyIn(Dist.CLIENT)
    private static int rarityColor(@NotNull LootEntry entry, @NotNull ItemStack stack) {
        if (entry.rarityColor() != -1) {
            return entry.rarityColor() & 0xFFFFFF;
        }
        String tier = entry.rarityTier() == null ? "consumer" : entry.rarityTier();
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

    @OnlyIn(Dist.CLIENT)
    private static void renderItemScaled(
            @NotNull GuiGraphics graphics,
            @NotNull ItemStack stack,
            int centerX,
            int centerY,
            float baseScale,
            float alpha,
            @NotNull LootEntry.ItemListTransform transform) {
        if (stack.isEmpty()) {
            return;
        }

        PoseStack pose = graphics.pose();

        /*
         * Use GuiGraphics#renderItem exactly as Minecraft does for inventory
         * items. This gives us the vanilla GUI ItemDisplayContext, foil/glint,
         * custom item renderers and GeckoLib/BEWLR handling.
         *
         * The surrounding LDLib screen normally has depth writes disabled, so
         * temporarily restore a normal item depth pass. This is important for
         * vanilla foil/glint and for real 3D custom item renderers.
         */
        graphics.flush();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clearDepth(1.0D);
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        pose.pushPose();
        try {
            pose.translate(
                    centerX + transform.offsetX(),
                    centerY + transform.offsetY(),
                    120.0F + transform.offsetZ()
            );

            if (transform.rotationX() != 0.0F) {
                pose.mulPose(Axis.XP.rotationDegrees(transform.rotationX()));
            }
            if (transform.rotationY() != 0.0F) {
                pose.mulPose(Axis.YP.rotationDegrees(transform.rotationY()));
            }
            if (transform.rotationZ() != 0.0F) {
                pose.mulPose(Axis.ZP.rotationDegrees(transform.rotationZ()));
            }

            pose.scale(
                    baseScale * transform.scaleX(),
                    baseScale * transform.scaleY(),
                    transform.scaleZ()
            );
            pose.translate(-8.0F, -8.0F, 0.0F);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

            /*
             * GuiGraphics#drawManaged is deprecated in 1.20.x. Its useful
             * behavior here is simply a flush before and after the draw.
             *
             * graphics.flush() ends GuiGraphics' own BufferSource batch,
             * including every fixed foil/glint RenderType, so the enchanted
             * pass is completed before any later LDLib panel is drawn.
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
    private static void blitTinted(@NotNull GuiGraphics graphics, @Nullable ResourceLocation texture, int x, int y, int width, int height, int rgb, float alpha) {
        if (texture == null) {
            return;
        }
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, alpha);
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static @NotNull String prettifyId(@NotNull String path) {
        String[] parts = path.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }

    @OnlyIn(Dist.CLIENT)
    private static @NotNull String ellipsize(@NotNull Font font, @NotNull String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "…";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (font.width(out.toString() + c) + ellipsisWidth > maxWidth) {
                break;
            }
            out.append(c);
        }
        return out.append(ellipsis).toString();
    }
}
