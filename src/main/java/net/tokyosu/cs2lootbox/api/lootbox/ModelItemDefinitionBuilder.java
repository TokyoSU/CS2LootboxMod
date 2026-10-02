package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** Static model item configuration, sharing the case renderer's display conventions. */
public final class ModelItemDefinitionBuilder {
    private final LootboxDefinitionBuilder display;
    private boolean hasModel, hasTexture;
    private LootRarityGrade grade = LootRarityGrade.MIL_SPEC;

    public ModelItemDefinitionBuilder(String id) {
        display = new LootboxDefinitionBuilder(id);
        display.caseItem(id).requiresKey(false).caseStackSize(64).identityItemTransforms()
                .animation("cs2lootbox:animations/static_item.animation.json");
    }
    public ModelItemDefinitionBuilder(ResourceLocation id) { this(id.toString()); }
    public ModelItemDefinitionBuilder model(String resource) { display.model(resource); hasModel = true; return this; }
    public ModelItemDefinitionBuilder texture(String resource) { display.texture(resource); hasTexture = true; return this; }
    public ModelItemDefinitionBuilder name(String key) { display.caseName(key); return this; }
    public ModelItemDefinitionBuilder stackSize(int size) { display.caseStackSize(size); return this; }
    public ModelItemDefinitionBuilder rarityGrade(LootRarityGrade value) { grade = Objects.requireNonNull(value); return this; }
    public ModelItemDefinitionBuilder rarityGrade(String value) { return rarityGrade(LootRarityGrade.fromId(value)); }
    public ModelItemDefinitionBuilder firstPersonRight(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.firstPersonRight(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder firstPersonLeft(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.firstPersonLeft(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder thirdPersonRight(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.thirdPersonRight(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder thirdPersonLeft(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.thirdPersonLeft(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder ground(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.ground(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder gui(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.gui(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder fixed(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.fixed(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder guiCarousel(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.guiCarousel(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder guiCarousel(float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) { display.guiCarousel(tx, ty, tz, rx, ry, rz, sx, sy, sz); return this; }
    public ModelItemDefinitionBuilder guiReveal(float tx, float ty, float tz, float rx, float ry, float rz, float scale) { display.guiReveal(tx, ty, tz, rx, ry, rz, scale); return this; }
    public ModelItemDefinitionBuilder guiReveal(float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) { display.guiReveal(tx, ty, tz, rx, ry, rz, sx, sy, sz); return this; }
    public Definition build() {
        if (!hasModel || !hasTexture) throw new IllegalStateException("Model items require model() and texture()");
        return new Definition(display.build(), grade);
    }
    public record Definition(LootboxDefinition display, LootRarityGrade grade) {}
}


