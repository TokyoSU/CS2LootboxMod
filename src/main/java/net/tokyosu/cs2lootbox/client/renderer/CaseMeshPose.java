package net.tokyosu.cs2lootbox.client.renderer;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;

/** Pose fingerprint taken after GeckoLib evaluates the item's animation. */
final class CaseMeshPose {
    private final GeoBone[] bones;
    private final float[] transforms;
    private final int[] flags;

    CaseMeshPose(BakedGeoModel model) {
        List<GeoBone> ordered = new ArrayList<>();
        collect(model.topLevelBones(), ordered);
        bones = ordered.toArray(GeoBone[]::new);
        transforms = new float[bones.length * 12];
        flags = new int[bones.length];
        for (int i = 0; i < bones.length; i++) {
            GeoBone bone = bones[i];
            int offset = i * 12;
            transforms[offset] = bone.getPosX();
            transforms[offset + 1] = bone.getPosY();
            transforms[offset + 2] = bone.getPosZ();
            transforms[offset + 3] = bone.getRotX();
            transforms[offset + 4] = bone.getRotY();
            transforms[offset + 5] = bone.getRotZ();
            transforms[offset + 6] = bone.getScaleX();
            transforms[offset + 7] = bone.getScaleY();
            transforms[offset + 8] = bone.getScaleZ();
            transforms[offset + 9] = bone.getPivotX();
            transforms[offset + 10] = bone.getPivotY();
            transforms[offset + 11] = bone.getPivotZ();
            flags[i] = flags(bone);
        }
    }

    boolean matches(BakedGeoModel model) {
        return matches(model.topLevelBones(), 0) == bones.length;
    }

    private int matches(List<GeoBone> list, int index) {
        for (int j = 0; j < list.size(); j++) {
            GeoBone bone = list.get(j);
            if (index >= bones.length || bone != bones[index] || flags[index] != flags(bone)) return -1;
            int offset = index * 12;
            if (transforms[offset] != bone.getPosX() || transforms[offset + 1] != bone.getPosY()
                    || transforms[offset + 2] != bone.getPosZ() || transforms[offset + 3] != bone.getRotX()
                    || transforms[offset + 4] != bone.getRotY() || transforms[offset + 5] != bone.getRotZ()
                    || transforms[offset + 6] != bone.getScaleX() || transforms[offset + 7] != bone.getScaleY()
                    || transforms[offset + 8] != bone.getScaleZ() || transforms[offset + 9] != bone.getPivotX()
                    || transforms[offset + 10] != bone.getPivotY() || transforms[offset + 11] != bone.getPivotZ()) return -1;
            index = matches(bone.getChildBones(), index + 1);
            if (index < 0) return -1;
        }
        return index;
    }

    static boolean hasTrackedBones(List<GeoBone> bones) {
        for (int i = 0; i < bones.size(); i++) {
            GeoBone bone = bones.get(i);
            if (bone.isTrackingMatrices() || hasTrackedBones(bone.getChildBones())) return true;
        }
        return false;
    }

    private static int flags(GeoBone bone) {
        return (bone.isHidden() ? 1 : 0) | (bone.isHidingChildren() ? 2 : 0);
    }

    private static void collect(List<GeoBone> bones, List<GeoBone> ordered) {
        for (GeoBone bone : bones) {
            ordered.add(bone);
            collect(bone.getChildBones(), ordered);
        }
    }
}
