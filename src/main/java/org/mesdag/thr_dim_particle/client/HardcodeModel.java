package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class HardcodeModel {
    private static final long fullModelColor = 0xFFFFFFFFL << 32;
    private static final long fullModelLight = 0xF000F0L << 24;
    protected final CompiledVertex[][] quads;

    public HardcodeModel(ModelPart root, ResourceLocation textureLocation) {
        List<CompiledVertex[]> list = new ArrayList<>();
        PoseStack poseStack = new PoseStack();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collectQuads(list, poseStack, root, TDPClient.getAtlas().getSprite(textureLocation));
        this.quads = list.toArray(new CompiledVertex[0][0]);
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
                    vertices[i] = new CompiledVertex(pos.x, pos.y, pos.z, sprite.getU(vertex.u), sprite.getV(vertex.v));
                }
                list.add(vertices);
            }
        }
        for (ModelPart child : part.children.values()) {
            collectQuads(list, poseStack, child, sprite);
        }
        poseStack.popPose();
    }

    // todo 提前分支预测
    public void renderToBuffer(TDParticle particle, Matrix4f pose, ParticleBuffer buffer, float vx, float vy, float vz) {
        float m00 = pose.m00(), m10 = pose.m10(), m20 = pose.m20(), m30 = pose.m30(),
                m01 = pose.m01(), m11 = pose.m11(), m21 = pose.m21(), m31 = pose.m31(),
                m02 = pose.m02(), m12 = pose.m12(), m22 = pose.m22(), m32 = pose.m32();
        boolean le = BufferBuilder.IS_LITTLE_ENDIAN;
        int c = particle.abgr;
        int l = particle.light;
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

            if (le) {
                l(vertex0, ptr, x0, y0, z0, c, l);
                l(vertex1, ptr += TDPRenderType.VERTEX_SIZE, x1, y1, z1, c, l);
                l(vertex2, ptr += TDPRenderType.VERTEX_SIZE, x2, y2, z2, c, l);
                l(vertex3, ptr += TDPRenderType.VERTEX_SIZE, x3, y3, z3, c, l);
            } else {
                b(vertex0, ptr, x0, y0, z0, c, l);
                b(vertex1, ptr += TDPRenderType.VERTEX_SIZE, x1, y1, z1, c, l);
                b(vertex2, ptr += TDPRenderType.VERTEX_SIZE, x2, y2, z2, c, l);
                b(vertex3, ptr += TDPRenderType.VERTEX_SIZE, x3, y3, z3, c, l);
            }
            ptr += TDPRenderType.VERTEX_SIZE;
        }
        buffer.popPtr(ptr);
    }

    private static void l(CompiledVertex vertex, long ptr, float x, float y, float z, int c, int l) {
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_X, x);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Y, y);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Z, z);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, fullModelColor | (c & 0xFFFFFFFFL));
        MemoryUtil.memPutLong(ptr + ParticleBuffer.MODEL_LIGHT, fullModelLight | l);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.U, vertex.u);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.V, vertex.v);
    }

    private static void b(CompiledVertex vertex, long ptr, float x, float y, float z, int c, int l) {
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_X, x);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Y, y);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Z, z);
        MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, Long.reverseBytes(fullModelColor | (c & 0xFFFFFFFFL)));
        MemoryUtil.memPutInt(ptr + ParticleBuffer.MODEL_LIGHT, 0); // 模型光照为0
        MemoryUtil.memPutShort(ptr + ParticleBuffer.ENV_LIGHT, (short) (l & 0xFFFF));
        MemoryUtil.memPutShort(ptr + ParticleBuffer.ENV_LIGHT_S, (short) (l >> 16 & 0xFFFF));
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.U, vertex.u);
        MemoryUtil.memPutFloat(ptr + ParticleBuffer.V, vertex.v);
    }

    public interface Renderer<M extends HardcodeModel> extends ModelRenderer<M> {}

    public record CompiledVertex(float x, float y, float z, float u, float v) {}
}
