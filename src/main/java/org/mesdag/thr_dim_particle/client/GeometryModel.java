package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class GeometryModel {
    private static final int[] ma = new int[256 * 256];
    private static final RandomSource rs = RandomSource.create();
    private static final float[][] pt4 = new float[4][3];
    private static final int[] starts = {0, 8, 16, 24};
    protected TDPRenderType renderType = TDPRenderType.get(0);
    protected final BakedModel model;

    public GeometryModel(BakedModel model) {
        this.model = model;
    }

    public void setRenderType(TDPRenderType renderType) {
        this.renderType = renderType;
    }

    public void renderToBuffer(Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz, int packedLight, int a, int r, int g, int b) {
        rs.setSeed(251129);
        for (BakedQuad quad : model.getQuads(null, null, rs)) {
            int[] vertices = quad.getVertices();
            float[] p3t;
            float x, y, z;

            for (int index = 0; index < 4; index++) {
                int start = starts[index];
                p3t = pt4[index];
                x = Float.intBitsToFloat(vertices[start]);
                y = Float.intBitsToFloat(vertices[start + 1]);
                z = Float.intBitsToFloat(vertices[start + 2]);
                p3t[0] = pose.m00() * x + pose.m10() * y + pose.m20() * z + pose.m30();
                p3t[1] = pose.m01() * x + pose.m11() * y + pose.m21() * z + pose.m31();
                p3t[2] = pose.m02() * x + pose.m12() * y + pose.m22() * z + pose.m32();
            }
            p3t = pt4[0];
            x = p3t[0];
            y = p3t[1];
            z = p3t[2];
            p3t = pt4[1];
            float vax = p3t[0] - x;
            float vay = p3t[1] - y;
            float vaz = p3t[2] - z;
            p3t = pt4[2];
            x = p3t[0] - x;
            y = p3t[1] - y;
            z = p3t[2] - z;
            if (vx * (vay * z - vaz * y) + vy * (vaz * x - vax * z) + vz * (vax * y - vay * x) >= 0) continue;

            for (int index = 0; index < 4; index++) {
                int start = starts[index];
                long ptr = buffer.beginVertex();

                // position
                p3t = pt4[index];
                MemoryUtil.memPutFloat(ptr, p3t[0]);
                MemoryUtil.memPutFloat(ptr + 4L, p3t[1]);
                MemoryUtil.memPutFloat(ptr + 8L, p3t[2]);
                // color & light
                int color = vertices[start + 3]; // argb格式
                color = ma[((color >>> 24) << 8) + a] << 24 |
                        ma[((color >> 16 & 0xFF) << 8) + r] |
                        ma[((color >> 8 & 0xFF) << 8) + g] << 8 |
                        ma[((color & 0xFF) << 8) + b] << 16; // 需要abgr格式
                if (BufferBuilder.IS_LITTLE_ENDIAN) {
                    MemoryUtil.memPutInt(ptr + 12L, color);
                    MemoryUtil.memPutInt(ptr + 24L, vertices[start + IQuadTransformer.UV2]);
                    MemoryUtil.memPutInt(ptr + 28L, packedLight);
                } else {
                    MemoryUtil.memPutInt(ptr + 12L, Integer.reverseBytes(color));
                    color = vertices[start + IQuadTransformer.UV2];
                    MemoryUtil.memPutShort(ptr + 24L, (short) (color & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 26L, (short) (color >> 16 & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 28L, (short) (packedLight & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 30L, (short) (packedLight >> 16 & 0xFFFF));
                }
                // uv
                MemoryUtil.memPutFloat(ptr + 16L, Float.intBitsToFloat(vertices[start + 4]));
                MemoryUtil.memPutFloat(ptr + 20L, Float.intBitsToFloat(vertices[start + 5]));
            }
        }
    }

    public interface Renderer<M extends GeometryModel> extends ModelRenderer<M> {
        @Override
        default TDPRenderType getRenderType(TDParticle particle) {
            return getModel().renderType;
        }
    }

    static {
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                ma[i * 256 + j] = (i * j) >> 8;
            }
        }
    }
}
