package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class GeometryModel {
    private static final int[] ma = new int[256 * 256];
    private static final RandomSource rs = RandomSource.create();
    private static final Vector3f pt = new Vector3f();
    protected TDPRenderType renderType = TDPRenderType.get(0);
    protected final BakedModel model;

    public GeometryModel(BakedModel model) {
        this.model = model;
    }

    public void setRenderType(TDPRenderType renderType) {
        this.renderType = renderType;
    }

    public void renderToBuffer(PoseStack poseStack, BufferBuilder buffer, int packedLight, int a, int r, int g, int b) {
        Matrix4f pose = poseStack.last().pose();
        rs.setSeed(251129);
        for (BakedQuad quad : model.getQuads(null, null, rs)) {
            int[] vertices = quad.getVertices();
            int size = vertices.length / IQuadTransformer.STRIDE;

            for (int index = 0; index < size; index++) {
                int start = index * IQuadTransformer.STRIDE;
                pt.set(
                        Float.intBitsToFloat(vertices[start]),
                        Float.intBitsToFloat(vertices[start + 1]),
                        Float.intBitsToFloat(vertices[start + 2])
                ).mulPosition(pose);

                long p = buffer.beginVertex();
                // position
                MemoryUtil.memPutFloat(p, pt.x);
                MemoryUtil.memPutFloat(p + 4L, pt.y);
                MemoryUtil.memPutFloat(p + 8L, pt.z);
                // color & light
                int color = vertices[start + 3]; // argb格式
                color = ma[((color >>> 24) << 8) + a] << 24 |
                        ma[((color >> 16 & 0xFF) << 8) + r] |
                        ma[((color >> 8 & 0xFF) << 8 ) + g] << 8 |
                        ma[((color & 0xFF) << 8) + b] << 16; // 需要abgr格式
                if (BufferBuilder.IS_LITTLE_ENDIAN) {
                    MemoryUtil.memPutInt(p + 12L, color);
                    MemoryUtil.memPutInt(p + 24L, vertices[start + IQuadTransformer.UV2]);
                    MemoryUtil.memPutInt(p + 28L, packedLight);
                } else {
                    MemoryUtil.memPutInt(p + 12L, Integer.reverseBytes(color));
                    color = vertices[start + IQuadTransformer.UV2];
                    MemoryUtil.memPutShort(p + 24L, (short) (color & 0xFFFF));
                    MemoryUtil.memPutShort(p + 26L, (short) (color >> 16 & 0xFFFF));
                    MemoryUtil.memPutShort(p + 28L, (short) (packedLight & 0xFFFF));
                    MemoryUtil.memPutShort(p + 30L, (short) (packedLight >> 16 & 0xFFFF));
                }
                // uv
                MemoryUtil.memPutFloat(p + 16L, Float.intBitsToFloat(vertices[start + 4]));
                MemoryUtil.memPutFloat(p + 20L, Float.intBitsToFloat(vertices[start + 5]));
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
