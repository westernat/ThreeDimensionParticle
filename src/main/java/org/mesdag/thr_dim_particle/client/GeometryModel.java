package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class GeometryModel {
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

    public void renderToBuffer(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz) {
        rs.setSeed(251209);
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
                buffer.vertices++;
                long ptr = buffer.buffer.reserve(buffer.vertexSize);
                buffer.vertexPointer = ptr;

                // position
                p3t = pt4[index];
                MemoryUtil.memPutFloat(ptr, p3t[0]);
                MemoryUtil.memPutFloat(ptr + 4L, p3t[1]);
                MemoryUtil.memPutFloat(ptr + 8L, p3t[2]);
                // color & light
                int color = vertices[start + 3]; // argb格式
                if (BufferBuilder.IS_LITTLE_ENDIAN) {
                    // color
                    MemoryUtil.memPutLong(ptr + 12L, ((long) color << 32) | (particle.argb & 0xFFFFFFFFL));
                    MemoryUtil.memPutLong(ptr + 24L + 4L, ((long) vertices[start + IQuadTransformer.UV2] << 32) | (particle.light & 0xFFFFFFFFL));
                } else {
                    // color
                    MemoryUtil.memPutLong(ptr + 12L, Long.reverseBytes(((long) color << 32) | (particle.argb & 0xFFFFFFFFL)));
                    // 借uv1存模型uv2
                    color = vertices[start + IQuadTransformer.UV2];
                    MemoryUtil.memPutShort(ptr + 24L + 4L, (short) (color & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 26L + 4L, (short) (color >> 16 & 0xFFFF));
                    // 环境uv2
                    MemoryUtil.memPutShort(ptr + 28L + 4L, (short) (particle.light & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 30L + 4L, (short) (particle.light >> 16 & 0xFFFF));
                }
                // uv0
                MemoryUtil.memPutInt(ptr + 16L + 4L, vertices[start + 4]);
                MemoryUtil.memPutInt(ptr + 20L + 4L, vertices[start + 5]);
            }
        }
    }

    public interface Renderer<M extends GeometryModel> extends ModelRenderer<M> {
        @Override
        default TDPRenderType getRenderType(TDParticle particle) {
            return getModel().renderType;
        }
    }
}
