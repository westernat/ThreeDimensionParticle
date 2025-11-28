package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class HardcodeModel { // todo
    protected final ModelPart root;
    protected TDPRenderType renderType;

    public HardcodeModel(ModelPart root) {
        this.root = root;
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public interface Renderer<M extends HardcodeModel> extends ModelRenderer<M> {
        ResourceLocation getTextureLocation(TDParticle particle);

        @Override
        default TDPRenderType getRenderType(TDParticle particle) {
            return getModel().renderType;
        }
    }
}
