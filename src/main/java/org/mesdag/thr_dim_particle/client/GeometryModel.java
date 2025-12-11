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
    private static final float[][] pt4 = new float[4][3];
    protected TDPRenderType renderType = TDPRenderType.get(0);
    public final CompiledVertex[][] quads;

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
                        (long) light << 24,
                        (short) (light & 0xFFFF),
                        (short) (light >> 16 & 0xFFFF),
                        vertices[start + 4],
                        vertices[start + 5]
                );
            }
            list.add(quad);
        }
        this.quads = list.toArray(new CompiledVertex[0][]);
    }

    public void setRenderType(TDPRenderType renderType) {
        this.renderType = renderType;
    }

    public void renderToBuffer(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz) {
        CompiledVertex vertex;
        float[] p3t;
        float x, y, z;
        l:
        for (CompiledVertex[] quad : quads) {
            for (int i = 0; i < 4; i++) {
                vertex = quad[i];
                x = vertex.x;
                y = vertex.y;
                z = vertex.z;
                p3t = pt4[i];
                p3t[0] = pose.m00() * x + pose.m10() * y + pose.m20() * z + pose.m30();
                p3t[1] = pose.m01() * x + pose.m11() * y + pose.m21() * z + pose.m31();
                p3t[2] = pose.m02() * x + pose.m12() * y + pose.m22() * z + pose.m32();

                if (i == 2) { // 先只算前三个顶点
                    float[] t = pt4[0];
                    x = t[0];
                    y = t[1];
                    z = t[2];
                    t = pt4[1];
                    float x1 = t[0] - x;
                    float y1 = t[1] - y;
                    float z1 = t[2] - z;
                    x = p3t[0] - x;
                    y = p3t[1] - y;
                    z = p3t[2] - z;
                    if (vx * (y1 * z - z1 * y) + vy * (z1 * x - x1 * z) + vz * (x1 * y - y1 * x) >= 0) continue l; // 背面剔除
                }
            }

            for (int i = 0; i < 4; i++) {
                vertex = quad[i];
                buffer.vertices++;
                long ptr = buffer.buffer.reserve(buffer.vertexSize);
                buffer.vertexPointer = ptr;

                // position
                p3t = pt4[i];
                MemoryUtil.memPutFloat(ptr, p3t[0]);
                MemoryUtil.memPutFloat(ptr + 4L, p3t[1]);
                MemoryUtil.memPutFloat(ptr + 8L, p3t[2]);
                // color & light
                if (BufferBuilder.IS_LITTLE_ENDIAN) {
                    // color
                    MemoryUtil.memPutLong(ptr + 12L, vertex.c | (particle.argb & 0xFFFFFFFFL));
                    // 环境uv2
                    MemoryUtil.memPutLong(ptr + 28L, vertex.l | (particle.light & 0xF000F0));
                } else {
                    // color
                    MemoryUtil.memPutLong(ptr + 12L, Long.reverseBytes(vertex.c | (particle.argb & 0xFFFFFFFFL)));
                    // 借uv1存模型uv2
                    MemoryUtil.memPutShort(ptr + 28L, vertex.a);
                    MemoryUtil.memPutShort(ptr + 30L, vertex.b);
                    // 环境uv2
                    MemoryUtil.memPutShort(ptr + 32L, (short) (particle.light & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 34L, (short) (particle.light >> 16 & 0xFFFF));
                }
                // uv0
                MemoryUtil.memPutInt(ptr + 20L, vertex.u);
                MemoryUtil.memPutInt(ptr + 24L, vertex.v);
            }
        }
    }

    public interface Renderer<M extends GeometryModel> extends ModelRenderer<M> {
        @Override
        default TDPRenderType getRenderType(TDParticle particle) {
            return getModel().renderType;
        }
    }

    /// @param x vertex x
    /// @param y vertex y
    /// @param z vertex z
    /// @param c model color
    /// @param l model light
    /// @param a little endian light A
    /// @param b little endian light B
    /// @param u vertex u
    /// @param v vertex v
    public record CompiledVertex(float x, float y, float z, long c, long l, short a, short b, int u, int v) {}
}
