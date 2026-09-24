package net.tokyosu.cs2lootbox.client.renderer.skinning;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Extra weighted-mesh payload embedded by the CS2 Blockbench importer inside a
 * normal GeckoLib .geo.json file as {@code geometry.cs2_skinning}.
 *
 * <p>The regular GeckoLib/GeckoMesh model remains authoritative for the bone
 * hierarchy and animation. This payload only stores the bind-pose vertices,
 * UVs and up to four bone influences per vertex.</p>
 */
public record SkinnedGeoModelData(@NotNull List<SkinnedMesh> meshes) {
    public static final SkinnedGeoModelData EMPTY = new SkinnedGeoModelData(List.of());

    public SkinnedGeoModelData {
        meshes = List.copyOf(meshes);
    }

    public boolean isEmpty() {
        return meshes.isEmpty();
    }

    public record SkinnedMesh(
            @NotNull String name,
            @NotNull String[] bones,
            @NotNull float[] positions,
            @NotNull float[] uvs,
            @NotNull int[] joints,
            @NotNull float[] weights,
            @NotNull int[] indices) {

        public int vertexCount() {
            return positions.length / 3;
        }

        public int triangleCount() {
            return indices.length / 3;
        }
    }
}
