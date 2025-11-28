package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GeometryModel {
    protected RenderType renderType = TDPClient.getParticleSolidRenderType();
    protected final BakedModel model;

    public GeometryModel(BakedModel model) {
        this.model = model;
    }

    public void setRenderType(RenderType renderType) {
        this.renderType = renderType;
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, float r, float g, float b, float a) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            Matrix4f pose = poseStack.last().pose();
            Vector3f position = new Vector3f();
            for (BakedQuad quad : model.getQuads(null, null, RandomSource.create(251128), ModelData.EMPTY, renderType)) {
                int[] vertices = quad.getVertices();
                int i = vertices.length / 8;

                for (int j = 0; j < i; j++) {
                    intBuffer.clear();
                    intBuffer.put(vertices, j * 8, 8);
                    pose.transformPosition(byteBuffer.getFloat(0), byteBuffer.getFloat(4), byteBuffer.getFloat(8), position);
                    buffer.addVertex(position.x(), position.y(), position.z())
                            .setColor((int) ((byteBuffer.get(12) & 255) * r), (int) ((byteBuffer.get(13) & 255) * g), (int) ((byteBuffer.get(14) & 255) * b), (int) ((byteBuffer.get(15) & 255) * a))
                            .setUv(byteBuffer.getFloat(16), byteBuffer.getFloat(20))
                            .setLight(buffer.applyBakedLighting(packedLight, byteBuffer))
                            .setUv1(0, 0)
                            .setNormal(0, 0, 0);
                }
            }
        }
    }

    public interface Renderer<M extends GeometryModel> extends ModelRenderer<M> {
        @Override
        default RenderType getRenderType(TDParticle particle) {
            return getModel().renderType;
        }
    }
}
