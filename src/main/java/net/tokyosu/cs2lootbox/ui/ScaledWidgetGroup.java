package net.tokyosu.cs2lootbox.ui;

import net.tokyosu.apocalypselib.menu.layout.CanvasTransform;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Unity-style "Scale With Screen Size" container for LDLib.
 *
 * Children are authored in a fixed reference coordinate system. Rendering is
 * uniformly scaled to fit the current logical Minecraft GUI and centered. All
 * mouse coordinates are transformed back into the same reference space before
 * LDLib dispatches them to children, so visual and clickable areas stay aligned.
 */
public final class ScaledWidgetGroup extends WidgetGroup {
    private static final int SAFE_MARGIN = 0;
    private static final int FULLSCREEN_BACKDROP = 0x60050505;

    private final int referenceWidth;
    private final int referenceHeight;

    public ScaledWidgetGroup(int referenceWidth, int referenceHeight) {
        super(0, 0, referenceWidth, referenceHeight);
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
    }

    @OnlyIn(Dist.CLIENT)
    private @NotNull CanvasTransform transform() {
        int screenWidth = gui != null ? gui.getScreenWidth() : 0;
        int screenHeight = gui != null ? gui.getScreenHeight() : 0;

        if (screenWidth <= 0 || screenHeight <= 0) {
            Minecraft minecraft = Minecraft.getInstance();
            screenWidth = minecraft.getWindow().getGuiScaledWidth();
            screenHeight = minecraft.getWindow().getGuiScaledHeight();
        }

        Position position = getPosition();
        return CanvasTransform.fit(screenWidth, screenHeight, referenceWidth, referenceHeight,
                position.x, position.y, SAFE_MARGIN, 0.05F);
    }

    @OnlyIn(Dist.CLIENT)
    private double referenceMouseX(double mouseX, @NotNull CanvasTransform transform) {
        return transform.referenceX(mouseX);
    }

    @OnlyIn(Dist.CLIENT)
    private double referenceMouseY(double mouseY, @NotNull CanvasTransform transform) {
        return transform.referenceY(mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    private void beginTransform(@NotNull GuiGraphics graphics, @NotNull CanvasTransform transform) {
        graphics.pose().pushPose();
        graphics.pose().translate(transform.translateX(), transform.translateY(), 0.0F);
        graphics.pose().scale(transform.scale(), transform.scale(), 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    private void endTransform(@NotNull GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        CanvasTransform transform = transform();
        int referenceMouseX = (int) Math.round(referenceMouseX(mouseX, transform));
        int referenceMouseY = (int) Math.round(referenceMouseY(mouseY, transform));

        // Cover the whole viewport, but keep the Minecraft world visible behind
        // the CS2 UI.  This is intentionally only a translucent dimmer; the
        // individual panels add their own local backgrounds where needed.
        graphics.fill(0, 0, transform.screenWidth(), transform.screenHeight(), FULLSCREEN_BACKDROP);

        beginTransform(graphics, transform);
        try {
            super.drawInBackground(graphics, referenceMouseX, referenceMouseY, partialTicks);
        } finally {
            endTransform(graphics);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        CanvasTransform transform = transform();
        int referenceMouseX = (int) Math.round(referenceMouseX(mouseX, transform));
        int referenceMouseY = (int) Math.round(referenceMouseY(mouseY, transform));

        beginTransform(graphics, transform);
        try {
            super.drawInForeground(graphics, referenceMouseX, referenceMouseY, partialTicks);
        } finally {
            endTransform(graphics);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawOverlay(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        CanvasTransform transform = transform();
        int referenceMouseX = (int) Math.round(referenceMouseX(mouseX, transform));
        int referenceMouseY = (int) Math.round(referenceMouseY(mouseY, transform));

        beginTransform(graphics, transform);
        try {
            super.drawOverlay(graphics, referenceMouseX, referenceMouseY, partialTicks);
        } finally {
            endTransform(graphics);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        CanvasTransform transform = transform();
        return super.mouseWheelMove(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform),
                wheelDelta
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        CanvasTransform transform = transform();
        return super.mouseClicked(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform),
                button
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        CanvasTransform transform = transform();
        return super.mouseDragged(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform),
                button,
                dragX / transform.scale(),
                dragY / transform.scale()
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        CanvasTransform transform = transform();
        return super.mouseReleased(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform),
                button
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseMoved(double mouseX, double mouseY) {
        CanvasTransform transform = transform();
        return super.mouseMoved(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform)
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        CanvasTransform transform = transform();
        return super.isMouseOverElement(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform)
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @Nullable Widget getHoverElement(double mouseX, double mouseY) {
        CanvasTransform transform = transform();
        return super.getHoverElement(
                referenceMouseX(mouseX, transform),
                referenceMouseY(mouseY, transform)
        );
    }

}
