package net.tokyosu.cs2lootbox.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;
import net.tokyosu.cs2lootbox.client.animation.LootboxAnimatable;
import net.tokyosu.cs2lootbox.client.model.LootboxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * GeckoLib renderer used by the LDLib lootbox preview.
 *
 * The regular Minecraft entity render types apply directional diffuse lighting
 * from the animated model normals. For this GUI preview that made the rotating
 * crate pulse between darker/brighter colours. Forge already ships an unlit
 * NEW_ENTITY-compatible render type, so use that instead of maintaining any
 * custom shader code. The lightmap value supplied by LootboxModelWidget still
 * controls the overall brightness.
 */
public final class LootboxGuiRenderer extends GeoObjectRenderer<LootboxAnimatable> {
    public LootboxGuiRenderer() {
        super(new LootboxModel());
    }

    @Override
    public @NotNull RenderType getRenderType(
            @NotNull LootboxAnimatable animatable,
            @NotNull ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        // Disable diffuse lighting from model normals and disable translucent
        // depth sorting. Both are unnecessary for the opaque case preview and
        // keeping the pass deterministic avoids visible brightness/sort changes
        // while the GeckoLib animation moves the model.
        return ForgeRenderTypes.getUnlitTranslucent(texture, false);
    }

    /**
     * GeoObjectRenderer is intended for generic animatables and GeckoLib
     * recommends giving each rendered object a stable instance id.
     */
    @Override
    public long getInstanceId(@NotNull LootboxAnimatable animatable) {
        return System.identityHashCode(animatable);
    }
}
