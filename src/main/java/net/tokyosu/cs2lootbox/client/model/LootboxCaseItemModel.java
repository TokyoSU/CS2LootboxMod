package net.tokyosu.cs2lootbox.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tokyosu.cs2lootbox.item.LootboxCaseItem;
import net.tokyosu.cs2lootbox.client.renderer.ClientRenderDiagnostics;
import net.tokyosu.cs2lootbox.client.renderer.ClientRenderRevision;
import net.tokyosu.cs2lootbox.client.renderer.StaticHeldCasePose;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import org.jetbrains.annotations.NotNull;

/**
 * Generic GeckoLib ItemStack model backed by the case's lootbox definition.
 */
public final class LootboxCaseItemModel extends GeoModel<LootboxCaseItem> {
    private final StaticHeldCasePose heldPose = new StaticHeldCasePose();
    private BakedGeoModel staticHeldModel;

    /** Armed only inside the rigid, static first-person renderer path. */
    public void setStaticHeldModel(BakedGeoModel model) { staticHeldModel = model; }

    @Override
    public void handleAnimations(LootboxCaseItem item, long instanceId, AnimationState<LootboxCaseItem> state) {
        if (staticHeldModel == null) {
            super.handleAnimations(item, instanceId, state);
            return;
        }
        Double tick = state.getData(DataTickets.TICK);
        double sampleTick = tick == null ? Double.NaN : tick;
        long revision = ClientRenderRevision.get();
        if (controllerAllowsReuse(item, instanceId)
                && heldPose.reuse(staticHeldModel, instanceId, revision, sampleTick)) {
            ClientRenderDiagnostics.heldPoseReused();
            return;
        }
        ClientRenderDiagnostics.heldPoseEvaluated();
        super.handleAnimations(item, instanceId, state);
        if (controllerAllowsReuse(item, instanceId)) heldPose.observe(staticHeldModel, instanceId, revision, sampleTick);
        else heldPose.clear();
    }

    private boolean controllerAllowsReuse(LootboxCaseItem item, long instanceId) {
        var controllers = item.getAnimatableInstanceCache().getManagerForId(instanceId).getAnimationControllers();
        var controller = controllers.get("item");
        if (controllers.size() != 1 || controller == null || controller.getTriggeredAnimation() != null
                || controller.getAnimationState() != AnimationController.State.RUNNING
                || controller.getCurrentAnimation() == null || controller.getCurrentRawAnimation() == null) return false;
        var stages = controller.getCurrentRawAnimation().getAnimationStages();
        String idle = item.getDefinition().animations().itemIdle();
        return stages.size() == 1 && stages.get(0).animationName().equals(idle)
                && stages.get(0).loopType() == Animation.LoopType.LOOP
                && controller.getCurrentAnimation().animation().name().equals(idle);
    }

    @Override
    public @NotNull ResourceLocation getModelResource(@NotNull LootboxCaseItem animatable) {
        return animatable.getDefinition().model();
    }

    @Override
    public @NotNull ResourceLocation getTextureResource(@NotNull LootboxCaseItem animatable) {
        return animatable.getDefinition().texture();
    }

    @Override
    public @NotNull ResourceLocation getAnimationResource(@NotNull LootboxCaseItem animatable) {
        return animatable.getDefinition().animation();
    }
}
