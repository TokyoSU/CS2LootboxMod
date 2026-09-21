package net.tokyosu.cs2lootbox.client.animation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtils;
import java.util.Map;
import java.util.Objects;

/**
 * Client-side GeckoLib state for one lootbox preview.
 */
public final class LootboxAnimatable implements GeoAnimatable {
    private final LootboxDefinition definition;
    private final RawAnimation fall;
    private final RawAnimation idle;
    private final RawAnimation open;
    private final RawAnimation openIdle;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private LootboxAnimationState animationState = LootboxAnimationState.FALL;
    private @Nullable TrackingAnimationController mainController;

    public LootboxAnimatable(@NotNull LootboxDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");

        LootboxDefinition.AnimationSet names = definition.animations();

        // Keep every lootbox state on its own GeckoLib clip. Finite state
        // transitions are driven by that clip's real Animation.length()
        // instead of chaining into the next clip and guessing when it ended.
        this.fall = RawAnimation.begin().thenPlayAndHold(names.fall());
        this.idle = RawAnimation.begin().thenPlayAndHold(names.idle());
        this.open = RawAnimation.begin().thenPlayAndHold(names.open());
        this.openIdle = names.hasOpenIdle()
                ? RawAnimation.begin().thenPlayAndHold(names.openIdle())
                : RawAnimation.begin().thenPlayAndHold(names.open());
    }

    public @NotNull LootboxDefinition getDefinition() {
        return definition;
    }

    @Override
    public void registerControllers(@NotNull AnimatableManager.ControllerRegistrar controllerRegistrar) {
        this.mainController = new TrackingAnimationController();
        controllerRegistrar.add(this.mainController);
    }

    public void setAnimationState(@NotNull LootboxAnimationState newState) {
        this.animationState = Objects.requireNonNull(newState, "newState");
    }

    public @NotNull LootboxAnimationState getAnimationState() {
        return animationState;
    }

    public @Nullable String getCurrentAnimationStage() {
        if (mainController == null || mainController.getCurrentAnimation() == null) {
            return null;
        }

        return mainController.getCurrentAnimation().animation().name();
    }

    public double getCurrentAnimationLength() {
        if (mainController == null || mainController.getCurrentAnimation() == null) {
            return 0.0D;
        }

        return mainController.getCurrentAnimation().animation().length();
    }

    /**
     * Returns the last animation tick that GeckoLib actually processed for the
     * currently loaded animation stage. This is intentionally render-driven:
     * UI state transitions must not guess animation progress from wall-clock
     * time because the controller can start later, pause, or use a speed
     * modifier.
     */
    public double getCurrentAnimationTick() {
        return mainController == null ? 0.0D : mainController.getLastProcessedAnimationTick();
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(@NotNull Object object) {
        return RenderUtils.getCurrentTick();
    }

    private @NotNull PlayState animationController(@NotNull AnimationState<LootboxAnimatable> state) {
        return switch (animationState) {
            case FALL -> state.setAndContinue(fall);
            case IDLE -> state.setAndContinue(idle);
            case OPEN -> state.setAndContinue(open);
            // If there is no distinct open_idle clip, OPEN is already held on
            // its exact last frame. Keep that controller timeline untouched so
            // changing the logical state cannot restart the open clip.
            case OPEN_IDLE -> definition.animations().hasOpenIdle()
                    ? state.setAndContinue(openIdle)
                    : PlayState.CONTINUE;
        };
    }

    /**
     * GeckoLib 4.8.4 does not expose the controller's current adjusted tick.
     * Track it immediately after GeckoLib processes each render frame so the UI
     * can compare the real playback position against Animation.length().
     */
    private final class TrackingAnimationController extends AnimationController<LootboxAnimatable> {
        private double lastProcessedAnimationTick;

        private TrackingAnimationController() {
            super(LootboxAnimatable.this, "main", 0, LootboxAnimatable.this::animationController);
        }

        @Override
        public void process(
                @NotNull CoreGeoModel<LootboxAnimatable> model,
                @NotNull AnimationState<LootboxAnimatable> state,
                @NotNull Map<String, CoreGeoBone> bones,
                @NotNull Map<String, BoneSnapshot> snapshots,
                double seekTime,
                boolean crashWhenCantFindBone) {
            super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);

            if (getCurrentAnimation() == null) {
                lastProcessedAnimationTick = 0.0D;
                return;
            }

            double animationLength = getCurrentAnimation().animation().length();
            if (getAnimationState() == State.PAUSED) {
                // HOLD_ON_LAST_FRAME sets PAUSED exactly when GeckoLib reaches
                // the end. Clamp to the real length so the UI sees a stable
                // completed frame rather than a wall-clock approximation.
                lastProcessedAnimationTick = animationLength;
                return;
            }

            // Same unit and equation used internally by GeckoLib's adjustTick().
            lastProcessedAnimationTick = getAnimationSpeed() * Math.max(seekTime - this.tickOffset, 0.0D);
        }

        private double getLastProcessedAnimationTick() {
            return lastProcessedAnimationTick;
        }
    }
}
