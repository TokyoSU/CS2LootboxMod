package net.tokyosu.cs2lootbox.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
public final class ModelItemTransforms {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);
    private ModelItemTransforms() {}
    public static @NotNull LootboxDefinition.ItemTransform selectItemTransform(
            @NotNull LootboxDefinition.ItemDisplayTransforms transforms,
            @NotNull ItemDisplayContext context) {
        return switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> transforms.firstPersonRight();
            case FIRST_PERSON_LEFT_HAND -> transforms.firstPersonLeft();
            case THIRD_PERSON_RIGHT_HAND -> transforms.thirdPersonRight();
            case THIRD_PERSON_LEFT_HAND -> transforms.thirdPersonLeft();
            case GROUND -> transforms.ground();
            case GUI -> transforms.gui();
            case FIXED -> transforms.fixed();
            default -> LootboxDefinition.ItemTransform.IDENTITY;
        };
    }

    public static boolean isLeftHandContext(@NotNull ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    /**
     * Applies the same conventions as vanilla ItemTransform.apply so values can
     * be moved 1:1 from a model JSON into KubeJS:
     * - translation is authored in model pixels and divided by 16
     * - left-hand transforms mirror X translation and Y/Z rotation
     * - rotation order is X -> Y -> Z, followed by scale
     *
     * Left and right still have fully independent KubeJS definitions; the
     * mirroring here only reproduces Minecraft's normal semantics for whichever
     * left-hand definition was selected.
     */
    public static void applyItemTransform(
            @NotNull PoseStack poseStack,
            @NotNull LootboxDefinition.ItemTransform transform,
            boolean leftHand) {
        float handSign = leftHand ? -1.0F : 1.0F;

        poseStack.translate(
                handSign * transform.translationX() / 16.0F,
                transform.translationY() / 16.0F,
                transform.translationZ() / 16.0F
        );

        if (transform.rotationX() != 0.0F
                || transform.rotationY() != 0.0F
                || transform.rotationZ() != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    transform.rotationX() * DEG_TO_RAD,
                    handSign * transform.rotationY() * DEG_TO_RAD,
                    handSign * transform.rotationZ() * DEG_TO_RAD
            ));
        }

        poseStack.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
    }
}

