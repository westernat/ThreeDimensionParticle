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
    private static final float[][] pt4 = new float[4][3];
    private static final long fullModelColor = 0xFFFFFFFFL << 32;
    private static final long fullModelLight = 0xF000F0L << 24;
    protected final CompiledVertex[][] quads;

    public HardcodeModel(ModelPart root, ResourceLocation textureLocation) {
        List<CompiledVertex[]> list = new ArrayList<>();
        PoseStack poseStack = new PoseStack();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collectQuads(list, poseStack, root, TDPClient.getAtlas().getSprite(textureLocation));
        this.quads = list.toArray(new CompiledVertex[0][]);
    }

    private static void collectQuads(List<CompiledVertex[]> list, PoseStack poseStack, ModelPart part, TextureAtlasSprite sprite) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Vector3f pos = new Vector3f();
        for (ModelPart.Cube cube : part.cubes) {
            for (ModelPart.Polygon polygon : cube.polygons) {
                CompiledVertex[] vertices = new CompiledVertex[4];
                for (int i = 0; i < 4; i++) {
                    ModelPart.Vertex vertex = polygon.vertices[i];
                    vertex.pos.mulPosition(poseStack.last().pose(), pos).div(16);
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
                    MemoryUtil.memPutLong(ptr + 12L, fullModelColor | (particle.argb & 0xFFFFFFFFL));
                    // uv2
                    MemoryUtil.memPutLong(ptr + 28L, fullModelLight | particle.light);
                } else {
                    // color
                    MemoryUtil.memPutLong(ptr + 12L, Long.reverseBytes(fullModelColor | (particle.argb & 0xFFFFFFFFL)));
                    // 借uv1存模型uv2
                    MemoryUtil.memPutInt(ptr + 28L, 0); // 模型光照为0
                    // 环境uv2
                    MemoryUtil.memPutShort(ptr + 32L, (short) (particle.light & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + 34L, (short) (particle.light >> 16 & 0xFFFF));
                }
                // uv0
                MemoryUtil.memPutFloat(ptr + 20L, vertex.u);
                MemoryUtil.memPutFloat(ptr + 24L, vertex.v);
            }
        }
    }

    public interface Renderer<M extends HardcodeModel> extends ModelRenderer<M> {}

    public record CompiledVertex(float x, float y, float z, float u, float v) {}
}
