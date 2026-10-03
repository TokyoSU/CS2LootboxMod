package net.tokyosu.cs2lootbox.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;

/** Unquantized local vertices: keep lighting, normals, tint and foil in the caller's render pass. */
final class LocalGeometry implements VertexConsumer {
    private float[] data = new float[1024];
    private int size;

    @Override public void vertex(float x, float y, float z, float r, float g, float b, float a,
                                 float u, float v, int overlay, int light, float nx, float ny, float nz) {
        if (size + 8 > data.length) data = Arrays.copyOf(data, data.length * 2);
        data[size++] = x; data[size++] = y; data[size++] = z;
        data[size++] = u; data[size++] = v;
        data[size++] = nx; data[size++] = ny; data[size++] = nz;
    }

    void finish() { data = Arrays.copyOf(data, size); }

    void emit(PoseStack pose, VertexConsumer out, int light, int overlay,
              float red, float green, float blue, float alpha) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int i = 0; i < size; i += 8) {
            float x = data[i], y = data[i + 1], z = data[i + 2];
            float nx = data[i + 5], ny = data[i + 6], nz = data[i + 7];
            out.vertex(m.m00() * x + m.m10() * y + m.m20() * z + m.m30(),
                    m.m01() * x + m.m11() * y + m.m21() * z + m.m31(),
                    m.m02() * x + m.m12() * y + m.m22() * z + m.m32(),
                    red, green, blue, alpha, data[i + 3], data[i + 4], overlay, light,
                    n.m00() * nx + n.m10() * ny + n.m20() * nz,
                    n.m01() * nx + n.m11() * ny + n.m21() * nz,
                    n.m02() * nx + n.m12() * ny + n.m22() * nz);
        }
    }

    public VertexConsumer vertex(double x, double y, double z) { throw new UnsupportedOperationException(); }
    public VertexConsumer color(int r, int g, int b, int a) { throw new UnsupportedOperationException(); }
    public VertexConsumer uv(float u, float v) { throw new UnsupportedOperationException(); }
    public VertexConsumer overlayCoords(int u, int v) { throw new UnsupportedOperationException(); }
    public VertexConsumer uv2(int u, int v) { throw new UnsupportedOperationException(); }
    public VertexConsumer normal(float x, float y, float z) { throw new UnsupportedOperationException(); }
    public void endVertex() { throw new UnsupportedOperationException(); }
    public void defaultColor(int r, int g, int b, int a) {}
    public void unsetDefaultColor() {}
}
