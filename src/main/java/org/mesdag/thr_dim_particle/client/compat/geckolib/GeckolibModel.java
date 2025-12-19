package org.mesdag.thr_dim_particle.client.compat.geckolib;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.mesdag.thr_dim_particle.client.ModelRenderer;
import org.mesdag.thr_dim_particle.client.ParticleBuffer;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.mesdag.thr_dim_particle.client.TDParticle;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class GeckolibModel {
    private static final float[][] pt4 = new float[4][3];
    private static final long fullModelColor = 0xFFFFFFFFL << 32;
    private static final long fullModelLight = 0xF000F0L << 24;
    protected final CompiledVertex[][] quads;

    public GeckolibModel(BakedGeoModel root, ResourceLocation textureLocation) {
        List<CompiledVertex[]> list = new ArrayList<>();
        PoseStack poseStack = new PoseStack();
        for (GeoBone bone : root.topLevelBones()) {
            collectQuads(list, poseStack, bone, TDPClient.getAtlas().getSprite(textureLocation));
        }
        this.quads = list.toArray(new CompiledVertex[0][0]);
    }

    /// @see GeoRenderer#renderRecursively(PoseStack, GeoAnimatable, GeoBone, RenderType, MultiBufferSource, VertexConsumer, boolean, float, int, int, int)
    private static void collectQuads(List<CompiledVertex[]> list, PoseStack poseStack, GeoBone bone, TextureAtlasSprite sprite) {
        poseStack.pushPose();
        RenderUtil.prepMatrixForBone(poseStack, bone);
        Vector3f pos = new Vector3f();
        for (GeoCube cube : bone.getCubes()) {
            poseStack.pushPose();
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
            Matrix4f pose = poseStack.last().pose();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                CompiledVertex[] vertices = new CompiledVertex[4];
                for (int i = 0; i < 4; i++) {
                    GeoVertex vertex = quad.vertices()[i];
                    vertex.position().mulPosition(pose, pos);
                    vertices[i] = new CompiledVertex(pos.x(), pos.y(), pos.z(), sprite.getU(vertex.texU()), sprite.getV(vertex.texV()));
                }
                list.add(vertices);
            }
            poseStack.popPose();
        }
        for (GeoBone childBone : bone.getChildBones()) {
            collectQuads(list, poseStack, childBone, sprite);
        }
        poseStack.popPose();
    }

    public void renderToBuffer(TDParticle particle, Matrix4f pose, ParticleBuffer buffer, float vx, float vy, float vz) {
        l:
        for (CompiledVertex[] quad : quads) {
            for (int i = 0; i < 4; i++) {
                CompiledVertex vertex = quad[i];
                float x = vertex.x;
                float y = vertex.y;
                float z = vertex.z;
                float[] p3t = pt4[i];
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
                CompiledVertex vertex = quad[i];
                long ptr = buffer.reserve();

                // position
                float[] p3t = pt4[i];
                MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_X, p3t[0]);
                MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Y, p3t[1]);
                MemoryUtil.memPutFloat(ptr + ParticleBuffer.POS_Z, p3t[2]);
                // color & light
                if (BufferBuilder.IS_LITTLE_ENDIAN) {
                    // color
                    MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, fullModelColor | (particle.abgr & 0xFFFFFFFFL));
                    // uv2
                    MemoryUtil.memPutLong(ptr + ParticleBuffer.MODEL_LIGHT, fullModelLight | particle.light);
                } else {
                    // color
                    MemoryUtil.memPutLong(ptr + ParticleBuffer.COLOR, Long.reverseBytes(fullModelColor | (particle.abgr & 0xFFFFFFFFL)));
                    // 借uv1存模型uv2
                    MemoryUtil.memPutInt(ptr + ParticleBuffer.MODEL_LIGHT, 0); // 模型光照为0
                    // 环境uv2
                    MemoryUtil.memPutShort(ptr + ParticleBuffer.ENV_LIGHT, (short) (particle.light & 0xFFFF));
                    MemoryUtil.memPutShort(ptr + ParticleBuffer.ENV_LIGHT_S, (short) (particle.light >> 16 & 0xFFFF));
                }
                // uv0
                MemoryUtil.memPutFloat(ptr + ParticleBuffer.U, vertex.u);
                MemoryUtil.memPutFloat(ptr + ParticleBuffer.V, vertex.v);
            }
        }
    }

    public interface Renderer<M extends GeckolibModel> extends ModelRenderer<M> {}

    public record CompiledVertex(float x, float y, float z, float u, float v) {}
}
