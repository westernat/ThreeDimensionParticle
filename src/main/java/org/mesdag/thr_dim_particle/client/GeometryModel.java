package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Arrays;

public class GeometryModel {
    protected static final Direction[] ALL_FACES_AND_NULL = Arrays.copyOf(Direction.values(), Direction.values().length + 1);
    protected RenderType renderType = TDPClient.getParticleSolidRenderType();
    protected final BakedModel model;

    public GeometryModel(BakedModel model) {
        this.model = model;
    }

    public void setRenderType(RenderType renderType) {
        this.renderType = renderType;
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, float a, float r, float g, float b) {
        RandomSource randomSource = RandomSource.create();
        for (Direction direction : ALL_FACES_AND_NULL) {
            randomSource.setSeed(251125);
            for (BakedQuad quad : model.getQuads(null, direction, randomSource, ModelData.EMPTY, renderType)) {
                buffer.putBulkData(poseStack.last(), quad, r, g, b, a, packedLight, OverlayTexture.NO_OVERLAY, true);
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
