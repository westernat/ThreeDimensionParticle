package org.mesdag.thr_dim_particle.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.IQuadTransformer;
import org.joml.Matrix4x3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GeometryModel {
    public final CompiledVertex[][] quads;
    public final int maxBytes;

    public GeometryModel(BakedModel model) {
        List<CompiledVertex[]> list = new ArrayList<>();
        List<BakedQuad> quadList = new ArrayList<>();
        RandomSource random = RandomSource.create(251225);
        for (Direction direction : Direction.values()) {
            quadList.addAll(model.getQuads(null, direction, random));
        }
        quadList.addAll(model.getQuads(null, null, random));
        for (BakedQuad bakedQuad : quadList) {
            int[] vertices = bakedQuad.getVertices();
            CompiledVertex[] quad = new CompiledVertex[4];
            for (int i = 0; i < 4; i++) {
                int start = i * IQuadTransformer.STRIDE;
                quad[i] = new CompiledVertex(
                        Float.intBitsToFloat(vertices[start]),
                        Float.intBitsToFloat(vertices[start + 1]),
                        Float.intBitsToFloat(vertices[start + 2]),
                        (long) vertices[start + 3] << 32,
                        (short) (TDPClient.light2Short(vertices[start + IQuadTransformer.UV2]) << 8),
                        ((long) vertices[start + 5] << 32) | (vertices[start + 4] & 0xFFFFFFFFL)
                );
            }
            list.add(quad);
        }
        this.quads = list.toArray(new CompiledVertex[0][0]);
        this.maxBytes = quads.length * 4 * TDPRenderType.VERTEX_SIZE;
    }

    public void renderToBuffer(TDParticle particle, Matrix4x3f pose, ParticleBuffer buffer) {
        int ptr = buffer.pushPtr(maxBytes);
        if (ptr == -1) return;
        ByteBuffer byteBuffer = buffer.buffer.buffer;
        float m00 = pose.m00(), m10 = pose.m10(), m20 = pose.m20(), m30 = pose.m30(),
                m01 = pose.m01(), m11 = pose.m11(), m21 = pose.m21(), m31 = pose.m31(),
                m02 = pose.m02(), m12 = pose.m12(), m22 = pose.m22(), m32 = pose.m32();
        long c = particle.abgr & 0xFFFFFFFFL;
        short l = particle.light;
        for (CompiledVertex[] quad : quads) {
            CompiledVertex vertex0 = quad[0];
            float xa = vertex0.x;
            float ya = vertex0.y;
            float za = vertex0.z;
            float x0 = Math.fma(m00, xa, Math.fma(m10, ya, Math.fma(m20, za, m30)));
            float y0 = Math.fma(m01, xa, Math.fma(m11, ya, Math.fma(m21, za, m31)));
            float z0 = Math.fma(m02, xa, Math.fma(m12, ya, Math.fma(m22, za, m32)));
            CompiledVertex vertex1 = quad[1];
            float xb = vertex1.x;
            float yb = vertex1.y;
            float zb = vertex1.z;
            float x1 = Math.fma(m00, xb, Math.fma(m10, yb, Math.fma(m20, zb, m30)));
            float y1 = Math.fma(m01, xb, Math.fma(m11, yb, Math.fma(m21, zb, m31)));
            float z1 = Math.fma(m02, xb, Math.fma(m12, yb, Math.fma(m22, zb, m32)));
            CompiledVertex vertex2 = quad[2];
            float xc = vertex2.x;
            float yc = vertex2.y;
            float zc = vertex2.z;
            float x2 = Math.fma(m00, xc, Math.fma(m10, yc, Math.fma(m20, zc, m30)));
            float y2 = Math.fma(m01, xc, Math.fma(m11, yc, Math.fma(m21, zc, m31)));
            float z2 = Math.fma(m02, xc, Math.fma(m12, yc, Math.fma(m22, zc, m32)));

            float x01 = x1 - x0;
            float y01 = y1 - y0;
            float z01 = z1 - z0;
            float x02 = x2 - x0;
            float y02 = y2 - y0;
            float z02 = z2 - z0;
            if (Math.fma(x2, Math.fma(y01, z02, -z01 * y02), Math.fma(y2, Math.fma(z01, x02, -x01 * z02), z2 * Math.fma(x01, y02, -y01 * x02))) < 0) { // 背面剔除
                CompiledVertex vertex3 = quad[3];
                float xd = vertex3.x;
                float yd = vertex3.y;
                float zd = vertex3.z;
                float x3 = Math.fma(m00, xd, Math.fma(m10, yd, Math.fma(m20, zd, m30)));
                float y3 = Math.fma(m01, xd, Math.fma(m11, yd, Math.fma(m21, zd, m31)));
                float z3 = Math.fma(m02, xd, Math.fma(m12, yd, Math.fma(m22, zd, m32)));

                v(byteBuffer, vertex0, ptr, x0, y0, z0, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
                v(byteBuffer, vertex1, ptr, x1, y1, z1, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
                v(byteBuffer, vertex2, ptr, x2, y2, z2, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
                v(byteBuffer, vertex3, ptr, x3, y3, z3, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
            }
        }
        buffer.popPtr(ptr);
    }

    private static void v(ByteBuffer buffer, CompiledVertex vertex, int ptr, float x, float y, float z, long c, short l) {
        buffer.putFloat(ptr + ParticleBuffer.POS_X, x);
        buffer.putFloat(ptr + ParticleBuffer.POS_Y, y);
        buffer.putFloat(ptr + ParticleBuffer.POS_Z, z);
        buffer.putLong(ptr + ParticleBuffer.COLOR, vertex.c | c);
        buffer.putLong(ptr + ParticleBuffer.UV, vertex.uv);
        buffer.putShort(ptr + ParticleBuffer.LIGHT, (short) (vertex.l | l));
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
