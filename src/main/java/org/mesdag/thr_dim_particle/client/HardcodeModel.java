package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4x3f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class HardcodeModel {
    private static final long fullModelColor = 0xFFFFFFFFL << 32;
    protected final CompiledVertex[][] quads;
    public final int maxBytes;

    public HardcodeModel(ModelPart root, ResourceLocation textureLocation) {
        List<CompiledVertex[]> list = new ArrayList<>();
        PoseStack poseStack = new PoseStack();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collectQuads(list, poseStack, root, TDPClient.getAtlas().getSprite(textureLocation));
        this.quads = list.toArray(new CompiledVertex[0][0]);
        this.maxBytes = quads.length * 4 * TDPRenderType.VERTEX_SIZE;
    }

    private static void collectQuads(List<CompiledVertex[]> list, PoseStack poseStack, ModelPart part, TextureAtlasSprite sprite) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Vector3f pos = new Vector3f();
        Matrix4f pose = poseStack.last().pose();
        for (ModelPart.Cube cube : part.cubes) {
            for (ModelPart.Polygon polygon : cube.polygons) {
                CompiledVertex[] vertices = new CompiledVertex[4];
                for (int i = 0; i < 4; i++) {
                    ModelPart.Vertex vertex = polygon.vertices[i];
                    vertex.pos.mulPosition(pose, pos).div(16);
                    int u = Float.floatToIntBits(sprite.getU(vertex.u));
                    int v = Float.floatToIntBits(sprite.getV(vertex.v));
                    vertices[i] = new CompiledVertex(pos.x, pos.y, pos.z, ((long) v << 32) | (u & 0xFFFFFFFFL));
                }
                list.add(vertices);
            }
        }
        for (ModelPart child : part.children.values()) {
            collectQuads(list, poseStack, child, sprite);
        }
        poseStack.popPose();
    }

    public void renderToBuffer(TDParticle particle, Matrix4x3f pose, ParticleBuffer buffer) {
        float m00 = pose.m00(), m10 = pose.m10(), m20 = pose.m20(), m30 = pose.m30(),
                m01 = pose.m01(), m11 = pose.m11(), m21 = pose.m21(), m31 = pose.m31(),
                m02 = pose.m02(), m12 = pose.m12(), m22 = pose.m22(), m32 = pose.m32();
        long c = particle.abgr & 0xFFFFFFFFL;
        short l = particle.light;
        long ptr = buffer.pushPtr(maxBytes);
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

                v(vertex0, ptr, x0, y0, z0, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
                v(vertex1, ptr, x1, y1, z1, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
                v(vertex2, ptr, x2, y2, z2, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
                v(vertex3, ptr, x3, y3, z3, c, l);
                ptr += TDPRenderType.VERTEX_SIZE;
            }
        }
        buffer.popPtr(ptr);
    }

    private static void v(CompiledVertex vertex, long ptr, float x, float y, float z, long c, short l) {
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_X, x);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Y, y);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Z, z);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, fullModelColor | c);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.UV, vertex.uv);
        MemoryUtil.memPutShort(ptr + ParticleBuffer.LIGHT, l);
    }

    public interface Renderer<M extends HardcodeModel> extends ModelRenderer<M> {}

    public record CompiledVertex(float x, float y, float z, long uv) {}
}
