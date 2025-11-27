package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class HardcodeModel extends Model {
    protected final ModelPart root;

    public HardcodeModel(Function<ResourceLocation, RenderType> renderType, ModelPart root) {
        super(renderType);
        this.root = root;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public interface Renderer<M extends HardcodeModel> extends ModelRenderer<M> {
        ResourceLocation getTextureLocation(TDParticle particle);

        @Override
        default RenderType getRenderType(TDParticle particle) {
            return getModel().renderType(getTextureLocation(particle));
        }
    }
}
