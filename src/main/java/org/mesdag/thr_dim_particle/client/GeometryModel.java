package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class GeometryModel {
    public final CompiledVertex[][] quads;
    public final int maxBytes;

    public GeometryModel(BakedModel model) {
        List<CompiledVertex[]> list = new ArrayList<>();
        for (BakedQuad bakedQuad : model.getQuads(null, null, RandomSource.create(251211))) {
            int[] vertices = bakedQuad.getVertices();
            CompiledVertex[] quad = new CompiledVertex[4];
            for (int i = 0; i < 4; i++) {
                int start = i * IQuadTransformer.STRIDE;
                int light = vertices[start + IQuadTransformer.UV2];
                quad[i] = new CompiledVertex(
                        Float.intBitsToFloat(vertices[start]),
                        Float.intBitsToFloat(vertices[start + 1]),
                        Float.intBitsToFloat(vertices[start + 2]),
                        (long) vertices[start + 3] << 32,
                        (short) (TDPClient.light2Short(light) << 8),
                        ((long) vertices[start + 5] << 32) | (vertices[start + 4] & 0xFFFFFFFFL)
                );
            }
            list.add(quad);
        }
        this.quads = list.toArray(new CompiledVertex[0][0]);
        this.maxBytes = quads.length * 4 * TDPRenderType.VERTEX_SIZE;
    }

    public void renderToBuffer(TDParticle particle, Matrix4f pose, ParticleBuffer buffer, float vx, float vy, float vz) {
        if (BufferBuilder.IS_LITTLE_ENDIAN) {
            l(particle, pose, buffer, vx, vy, vz);
        } else {
            b(particle, pose, buffer, vx, vy, vz);
        }
    }

    private void l(TDParticle particle, Matrix4f pose, ParticleBuffer buffer, float vx, float vy, float vz) {
        float m00 = pose.m00(), m10 = pose.m10(), m20 = pose.m20(), m30 = pose.m30(),
                m01 = pose.m01(), m11 = pose.m11(), m21 = pose.m21(), m31 = pose.m31(),
                m02 = pose.m02(), m12 = pose.m12(), m22 = pose.m22(), m32 = pose.m32();
        long c = particle.abgr & 0xFFFFFFFFL;
        short l = particle.light;
        long ptr = buffer.pushPtr(maxBytes);
        for (CompiledVertex[] quad : quads) {
            CompiledVertex vertex0 = quad[0];
            float x = vertex0.x;
            float y = vertex0.y;
            float z = vertex0.z;
            float x0 = Math.fma(m00, x, Math.fma(m10, y, Math.fma(m20, z, m30)));
            float y0 = Math.fma(m01, x, Math.fma(m11, y, Math.fma(m21, z, m31)));
            float z0 = Math.fma(m02, x, Math.fma(m12, y, Math.fma(m22, z, m32)));
            CompiledVertex vertex1 = quad[1];
            x = vertex1.x;
            y = vertex1.y;
            z = vertex1.z;
            float x1 = Math.fma(m00, x, Math.fma(m10, y, Math.fma(m20, z, m30)));
            float y1 = Math.fma(m01, x, Math.fma(m11, y, Math.fma(m21, z, m31)));
            float z1 = Math.fma(m02, x, Math.fma(m12, y, Math.fma(m22, z, m32)));
            CompiledVertex vertex2 = quad[2];
            x = vertex2.x;
            y = vertex2.y;
            z = vertex2.z;
            float x2 = Math.fma(m00, x, Math.fma(m10, y, Math.fma(m20, z, m30)));
            float y2 = Math.fma(m01, x, Math.fma(m11, y, Math.fma(m21, z, m31)));
            float z2 = Math.fma(m02, x, Math.fma(m12, y, Math.fma(m22, z, m32)));

            float x01 = x1 - x0;
            float y01 = y1 - y0;
            float z01 = z1 - z0;
            float x02 = x2 - x0;
            float y02 = y2 - y0;
            float z02 = z2 - z0;
            if (Math.fma(vx, Math.fma(y01, z02, -z01 * y02), Math.fma(vy, Math.fma(z01, x02, -x01 * z02), vz * Math.fma(x01, y02, -y01 * x02))) < 0) { // 背面剔除
                CompiledVertex vertex3 = quad[3];
                x = vertex3.x;
                y = vertex3.y;
                z = vertex3.z;
                float x3 = Math.fma(m00, x, Math.fma(m10, y, Math.fma(m20, z, m30)));
                float y3 = Math.fma(m01, x, Math.fma(m11, y, Math.fma(m21, z, m31)));
                float z3 = Math.fma(m02, x, Math.fma(m12, y, Math.fma(m22, z, m32)));

                l(vertex0, ptr, x0, y0, z0, c, l);
                l(vertex1, ptr += TDPRenderType.VERTEX_SIZE, x1, y1, z1, c, l);
                l(vertex2, ptr += TDPRenderType.VERTEX_SIZE, x2, y2, z2, c, l);
                l(vertex3, ptr += TDPRenderType.VERTEX_SIZE, x3, y3, z3, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
            }
        }
        buffer.popPtr(ptr);
    }

    private static void l(CompiledVertex vertex, long ptr, float x, float y, float z, long c, short l) {
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_X, x);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Y, y);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Z, z);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, vertex.c | c);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.UV, vertex.uv);
        MemoryUtil.memPutShort(ptr + ParticleBuffer.LIGHT, (short) (vertex.l | l));
    }

    private void b(TDParticle particle, Matrix4f pose, ParticleBuffer buffer, float vx, float vy, float vz) {
        float m00 = pose.m00(), m10 = pose.m10(), m20 = pose.m20(), m30 = pose.m30(),
                m01 = pose.m01(), m11 = pose.m11(), m21 = pose.m21(), m31 = pose.m31(),
                m02 = pose.m02(), m12 = pose.m12(), m22 = pose.m22(), m32 = pose.m32();
        long c = particle.abgr & 0xFFFFFFFFL;
        short l = particle.light;
        long ptr = buffer.pushPtr(quads.length * 4 * TDPRenderType.VERTEX_SIZE);
        for (CompiledVertex[] quad : quads) {
            CompiledVertex vertex0 = quad[0];
            float x = vertex0.x;
            float y = vertex0.y;
            float z = vertex0.z;
            float x0 = m00 * x + m10 * y + m20 * z + m30;
            float y0 = m01 * x + m11 * y + m21 * z + m31;
            float z0 = m02 * x + m12 * y + m22 * z + m32;
            CompiledVertex vertex1 = quad[1];
            x = vertex1.x;
            y = vertex1.y;
            z = vertex1.z;
            float x1 = m00 * x + m10 * y + m20 * z + m30;
            float y1 = m01 * x + m11 * y + m21 * z + m31;
            float z1 = m02 * x + m12 * y + m22 * z + m32;
            CompiledVertex vertex2 = quad[2];
            x = vertex2.x;
            y = vertex2.y;
            z = vertex2.z;
            float x2 = m00 * x + m10 * y + m20 * z + m30;
            float y2 = m01 * x + m11 * y + m21 * z + m31;
            float z2 = m02 * x + m12 * y + m22 * z + m32;

            float x01 = x1 - x0;
            float y01 = y1 - y0;
            float z01 = z1 - z0;
            float x02 = x2 - x0;
            float y02 = y2 - y0;
            float z02 = z2 - z0;
            if (vx * (y01 * z02 - z01 * y02) + vy * (z01 * x02 - x01 * z02) + vz * (x01 * y02 - y01 * x02) >= 0) continue; // 背面剔除

            CompiledVertex vertex3 = quad[3];
            x = vertex3.x;
            y = vertex3.y;
            z = vertex3.z;
            float x3 = m00 * x + m10 * y + m20 * z + m30;
            float y3 = m01 * x + m11 * y + m21 * z + m31;
            float z3 = m02 * x + m12 * y + m22 * z + m32;

            b(vertex0, ptr, x0, y0, z0, c, l);
            b(vertex1, ptr += TDPRenderType.VERTEX_SIZE, x1, y1, z1, c, l);
            b(vertex2, ptr += TDPRenderType.VERTEX_SIZE, x2, y2, z2, c, l);
            b(vertex3, ptr += TDPRenderType.VERTEX_SIZE, x3, y3, z3, c, l);
            ptr += TDPRenderType.VERTEX_SIZE;
        }
        buffer.popPtr(ptr);
    }

    private static void b(CompiledVertex vertex, long ptr, float x, float y, float z, long c, short l) {
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_X, x);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Y, y);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Z, z);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, Long.reverseBytes(vertex.c | c));
        MemoryUtil.memPutLong(ptr + ParticleBuffer.UV, Long.reverseBytes(vertex.uv));
        MemoryUtil.memPutShort(ptr + ParticleBuffer.LIGHT, (short) (vertex.l | l));
    }

    public interface Renderer<M extends GeometryModel> extends ModelRenderer<M> {}

    /// @param x  vertex x
    /// @param y  vertex y
    /// @param z  vertex z
    /// @param c  model color
    /// @param l  model light
    /// @param uv vertex uv
    public record CompiledVertex(float x, float y, float z, long c, short l, long uv) {}
}
