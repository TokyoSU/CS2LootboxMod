package net.tokyosu.cs2lootbox.client.renderer.skinning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * CPU linear-blend skinning driven by GeckoLib's already-evaluated GeoBone pose.
 *
 * <p>This intentionally does not create a second animation system. GeckoLib is
 * still responsible for controllers, easing, keyframes, hierarchy and the
 * current bone transforms. We only blend bind-pose vertices using those current
 * transforms before submitting them to the same VertexConsumer used by the
 * normal GeckoLib renderer.</p>
 */
public final class GeckoLibWeightedSkinRenderer {
    private static final float EPSILON = 1.0E-8F;

    private GeckoLibWeightedSkinRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            BakedGeoModel model,
            SkinnedGeoModelData data,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        if (data == null || data.isEmpty()) {
            return;
        }

        Map<String, Matrix4f> skinMatrices = new HashMap<>();
        Matrix4f identity = new Matrix4f();
        for (GeoBone root : model.topLevelBones()) {
            collectSkinMatrices(root, identity, identity, skinMatrices);
        }

        Matrix4f renderPose = new Matrix4f(poseStack.last().pose());
        Matrix3f renderNormal = new Matrix3f(poseStack.last().normal());

        for (SkinnedGeoModelData.SkinnedMesh mesh : data.meshes()) {
            renderMesh(mesh, skinMatrices, renderPose, renderNormal, buffer,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static void collectSkinMatrices(
            GeoBone bone,
            Matrix4f parentCurrent,
            Matrix4f parentBind,
            Map<String, Matrix4f> output) {
        Matrix4f current = applyBoneTransform(
                new Matrix4f(parentCurrent),
                bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                bone.getScaleX(), bone.getScaleY(), bone.getScaleZ(),
                bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());

        BoneSnapshot initial = bone.getInitialSnapshot();
        Matrix4f bind;
        if (initial != null) {
            bind = applyBoneTransform(
                    new Matrix4f(parentBind),
                    initial.getOffsetX(), initial.getOffsetY(), initial.getOffsetZ(),
                    initial.getRotX(), initial.getRotY(), initial.getRotZ(),
                    initial.getScaleX(), initial.getScaleY(), initial.getScaleZ(),
                    bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
        } else {
            // This should only happen before GeckoLib registers the active model.
            // Using the current pose as bind pose is safer than producing NaNs.
            bind = new Matrix4f(current);
        }

        Matrix4f inverseBind = new Matrix4f(bind);
        if (Math.abs(inverseBind.determinant()) > EPSILON) {
            inverseBind.invert();
            output.put(bone.getName(), new Matrix4f(current).mul(inverseBind));
        } else {
            output.put(bone.getName(), new Matrix4f());
        }

        for (GeoBone child : bone.getChildBones()) {
            collectSkinMatrices(child, current, bind, output);
        }
    }

    /** Mirrors software.bernie.geckolib.util.RenderUtils.prepMatrixForBone. */
    private static Matrix4f applyBoneTransform(
            Matrix4f matrix,
            float posX,
            float posY,
            float posZ,
            float rotX,
            float rotY,
            float rotZ,
            float scaleX,
            float scaleY,
            float scaleZ,
            float pivotX,
            float pivotY,
            float pivotZ) {
        matrix.translate(-posX / 16.0F, posY / 16.0F, posZ / 16.0F);
        matrix.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);

        if (rotZ != 0.0F) {
            matrix.rotateZ(rotZ);
        }
        if (rotY != 0.0F) {
            matrix.rotateY(rotY);
        }
        if (rotX != 0.0F) {
            matrix.rotateX(rotX);
        }

        matrix.scale(scaleX, scaleY, scaleZ);
        matrix.translate(-pivotX / 16.0F, -pivotY / 16.0F, -pivotZ / 16.0F);
        return matrix;
    }

    private static void renderMesh(
            SkinnedGeoModelData.SkinnedMesh mesh,
            Map<String, Matrix4f> matrices,
            Matrix4f renderPose,
            Matrix3f renderNormal,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        int vertexCount = mesh.vertexCount();
        float[] skinned = new float[vertexCount * 3];
        String[] bones = mesh.bones();
        float[] positions = mesh.positions();
        int[] joints = mesh.joints();
        float[] weights = mesh.weights();

        // Resolve the compact per-mesh joint table once, not once per vertex.
        Matrix4f[] jointMatrices = new Matrix4f[bones.length];
        for (int i = 0; i < bones.length; i++) {
            jointMatrices[i] = matrices.get(bones[i]);
        }

        Vector4f source = new Vector4f();
        Vector4f transformed = new Vector4f();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int p = vertex * 3;
            int w = vertex * 4;
            source.set(positions[p], positions[p + 1], positions[p + 2], 1.0F);

            float x = 0.0F;
            float y = 0.0F;
            float z = 0.0F;
            float sum = 0.0F;

            for (int influence = 0; influence < 4; influence++) {
                float weight = weights[w + influence];
                if (weight <= EPSILON) {
                    continue;
                }

                int joint = joints[w + influence];
                Matrix4f matrix = joint >= 0 && joint < jointMatrices.length
                        ? jointMatrices[joint]
                        : null;

                if (matrix == null) {
                    // A missing animation bone should not make the geometry vanish.
                    x += source.x * weight;
                    y += source.y * weight;
                    z += source.z * weight;
                    sum += weight;
                    continue;
                }

                transformed.set(source);
                matrix.transform(transformed);
                x += transformed.x * weight;
                y += transformed.y * weight;
                z += transformed.z * weight;
                sum += weight;
            }

            if (sum <= EPSILON) {
                x = source.x;
                y = source.y;
                z = source.z;
            } else if (Math.abs(sum - 1.0F) > 1.0E-4F) {
                x /= sum;
                y /= sum;
                z /= sum;
            }

            skinned[p] = x;
            skinned[p + 1] = y;
            skinned[p + 2] = z;
        }

        int[] indices = mesh.indices();
        float[] uvs = mesh.uvs();
        Vector3f normal = new Vector3f();
        Vector4f rendered = new Vector4f();

        for (int triangle = 0; triangle + 2 < indices.length; triangle += 3) {
            int i0 = indices[triangle];
            int i1 = indices[triangle + 1];
            int i2 = indices[triangle + 2];

            int p0 = i0 * 3;
            int p1 = i1 * 3;
            int p2 = i2 * 3;

            float e1x = skinned[p1] - skinned[p0];
            float e1y = skinned[p1 + 1] - skinned[p0 + 1];
            float e1z = skinned[p1 + 2] - skinned[p0 + 2];
            float e2x = skinned[p2] - skinned[p0];
            float e2y = skinned[p2 + 1] - skinned[p0 + 1];
            float e2z = skinned[p2 + 2] - skinned[p0 + 2];

            normal.set(
                    e1y * e2z - e1z * e2y,
                    e1z * e2x - e1x * e2z,
                    e1x * e2y - e1y * e2x);
            if (normal.lengthSquared() <= EPSILON) {
                normal.set(0.0F, 1.0F, 0.0F);
            } else {
                normal.normalize();
            }
            renderNormal.transform(normal);
            if (normal.lengthSquared() > EPSILON) {
                normal.normalize();
            }

            // GeckoLib's entity-style RenderTypes use QUADS. The importer has
            // already reversed the source triangle to the same winding as
            // GeckoMesh, so submit it as a degenerate quad by repeating the
            // last corner. Sending only three vertices would desynchronise the
            // following faces in a quad buffer.
            emitVertex(buffer, renderPose, rendered, skinned, uvs, i0, normal,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(buffer, renderPose, rendered, skinned, uvs, i1, normal,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(buffer, renderPose, rendered, skinned, uvs, i2, normal,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(buffer, renderPose, rendered, skinned, uvs, i2, normal,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static void emitVertex(
            VertexConsumer buffer,
            Matrix4f renderPose,
            Vector4f rendered,
            float[] positions,
            float[] uvs,
            int vertex,
            Vector3f normal,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        int p = vertex * 3;
        rendered.set(positions[p], positions[p + 1], positions[p + 2], 1.0F);
        renderPose.transform(rendered);

        int uv = vertex * 2;
        buffer.vertex(
                rendered.x(), rendered.y(), rendered.z(),
                red, green, blue, alpha,
                uvs[uv], uvs[uv + 1],
                packedOverlay, packedLight,
                normal.x(), normal.y(), normal.z());
    }

}
