package net.tokyosu.cs2lootbox.api.lootbox;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable description of one lootbox type.
 *
 * Definitions are created during KubeJS startup and are then shared by the
 * item renderer, LDLib preview widget and opening flow.
 */
public record LootboxDefinition(
        @NotNull ResourceLocation id,
        @NotNull ResourceLocation caseItemId,
        @NotNull ResourceLocation keyItemId,
        @NotNull ResourceLocation model,
        @NotNull ResourceLocation texture,
        @NotNull ResourceLocation animation,
        @NotNull ResourceLocation keyTexture,
        @NotNull ResourceLocation itemJson,
        @NotNull ResourceLocation openSound,
        @NotNull ResourceLocation carouselTickSound,
        @NotNull ResourceLocation rewardSound,
        @NotNull ResourceLocation uncommonSound,
        @NotNull ResourceLocation rareSound,
        @NotNull ResourceLocation mythicalSound,
        @NotNull ResourceLocation classifiedSound,
        @NotNull ResourceLocation covertSound,
        @NotNull ResourceLocation specialSound,
        @NotNull SoundProfile soundProfile,
        @NotNull AnimationSet animations,
        @NotNull PreviewTransform preview,
        @NotNull UiSkin uiSkin,
        int caseStackSize,
        int keyStackSize,
        @NotNull String caseTranslationKey,
        @NotNull String keyTranslationKey,
        @Nullable String resultCollectionTranslationKey,
        @Nullable ResourceLocation resultCollectionIconTexture,
        @NotNull List<LootEntry> loot,
        @NotNull List<LegendaryLoot> legendaryLoot) {

    public LootboxDefinition {
        id = Objects.requireNonNull(id, "id");
        caseItemId = Objects.requireNonNull(caseItemId, "caseItemId");
        keyItemId = Objects.requireNonNull(keyItemId, "keyItemId");
        model = Objects.requireNonNull(model, "model");
        texture = Objects.requireNonNull(texture, "texture");
        animation = Objects.requireNonNull(animation, "animation");
        keyTexture = Objects.requireNonNull(keyTexture, "keyTexture");
        itemJson = Objects.requireNonNull(itemJson, "itemJson");
        openSound = Objects.requireNonNull(openSound, "openSound");
        carouselTickSound = Objects.requireNonNull(carouselTickSound, "carouselTickSound");
        rewardSound = Objects.requireNonNull(rewardSound, "rewardSound");
        uncommonSound = Objects.requireNonNull(uncommonSound, "uncommonSound");
        rareSound = Objects.requireNonNull(rareSound, "rareSound");
        mythicalSound = Objects.requireNonNull(mythicalSound, "mythicalSound");
        classifiedSound = Objects.requireNonNull(classifiedSound, "classifiedSound");
        covertSound = Objects.requireNonNull(covertSound, "covertSound");
        specialSound = Objects.requireNonNull(specialSound, "specialSound");
        soundProfile = Objects.requireNonNull(soundProfile, "soundProfile");
        animations = Objects.requireNonNull(animations, "animations");
        preview = Objects.requireNonNull(preview, "preview");
        uiSkin = Objects.requireNonNull(uiSkin, "uiSkin");
        caseTranslationKey = Objects.requireNonNull(caseTranslationKey, "caseTranslationKey");
        keyTranslationKey = Objects.requireNonNull(keyTranslationKey, "keyTranslationKey");
        loot = List.copyOf(Objects.requireNonNull(loot, "loot"));
        legendaryLoot = List.copyOf(Objects.requireNonNull(legendaryLoot, "legendaryLoot"));
    }


    /**
     * Volume/pitch tuning for one UI sound. Volume is clamped to 0..1 and
     * pitch to 0.01..2 so malformed KubeJS values cannot reach the sound engine.
     */
    public record SoundTuning(float volume, float pitch) {
        public SoundTuning {
            volume = clamp(volume, 0.0F, 1.0F, 1.0F);
            pitch = clamp(pitch, 0.01F, 2.0F, 1.0F);
        }

        private static float clamp(float value, float min, float max, float fallback) {
            if (!Float.isFinite(value)) {
                return fallback;
            }
            return Math.max(min, Math.min(max, value));
        }
    }

    /**
     * Per-case sound tuning. A null cue override means that the cue inherits
     * {@link #defaults}. This makes changeCase(...).defaultSound(...) affect all
     * sounds that were not explicitly given their own volume/pitch.
     */
    public record SoundProfile(
            @NotNull SoundTuning defaults,
            @Nullable SoundTuning open,
            @Nullable SoundTuning carouselTick,
            @Nullable SoundTuning reward,
            @Nullable SoundTuning uncommon,
            @Nullable SoundTuning rare,
            @Nullable SoundTuning mythical,
            @Nullable SoundTuning classified,
            @Nullable SoundTuning covert,
            @Nullable SoundTuning special) {
        public SoundProfile {
            defaults = Objects.requireNonNull(defaults, "defaults");
        }

        public @NotNull SoundTuning resolve(@Nullable SoundTuning override) {
            return override != null ? override : defaults;
        }

        public @NotNull SoundTuning openResolved() { return resolve(open); }
        public @NotNull SoundTuning carouselTickResolved() { return resolve(carouselTick); }
        public @NotNull SoundTuning rewardResolved() { return resolve(reward); }
        public @NotNull SoundTuning uncommonResolved() { return resolve(uncommon); }
        public @NotNull SoundTuning rareResolved() { return resolve(rare); }
        public @NotNull SoundTuning mythicalResolved() { return resolve(mythical); }
        public @NotNull SoundTuning classifiedResolved() { return resolve(classified); }
        public @NotNull SoundTuning covertResolved() { return resolve(covert); }
        public @NotNull SoundTuning specialResolved() { return resolve(special); }
    }

    public record AnimationSet(
            @NotNull String fall,
            @NotNull String idle,
            @NotNull String open,
            @Nullable String openIdle,
            @NotNull String itemIdle) {
        public AnimationSet {
            fall = Objects.requireNonNull(fall, "fall");
            idle = Objects.requireNonNull(idle, "idle");
            open = Objects.requireNonNull(open, "open");
            openIdle = openIdle == null || openIdle.isBlank() ? null : openIdle.trim();
            itemIdle = Objects.requireNonNull(itemIdle, "itemIdle");
        }

        public boolean hasOpenIdle() {
            return openIdle != null;
        }
    }

    /**
     * Transform used by the 3D case inside the LDLib widget.
     */
    public record PreviewTransform(
            float offsetX,
            float offsetY,
            float scale,
            float pitch,
            float yaw,
            float roll,
            int blockLight,
            int skyLight) {
    }

    /** Optional customizable UI skin resources for the CS2-style screen. */
    public record UiSkin(
            @NotNull ResourceLocation slotBorderTexture,
            @NotNull ResourceLocation markerBarTexture,
            @NotNull ResourceLocation markerOuterCircleTexture,
            @NotNull ResourceLocation markerInnerCircleTexture) {
        public UiSkin {
            slotBorderTexture = Objects.requireNonNull(slotBorderTexture, "slotBorderTexture");
            markerBarTexture = Objects.requireNonNull(markerBarTexture, "markerBarTexture");
            markerOuterCircleTexture = Objects.requireNonNull(markerOuterCircleTexture, "markerOuterCircleTexture");
            markerInnerCircleTexture = Objects.requireNonNull(markerInnerCircleTexture, "markerInnerCircleTexture");
        }
    }
}
