package org.mesdag.thr_dim_particle.client.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.mesdag.thr_dim_particle.client.GeometryModel;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class SimpleGeometryModelRenderer implements GeometryModel.Renderer<GeometryModel> {
    protected final GeometryModel model;

    public SimpleGeometryModelRenderer(EntityRendererProvider.Context context, ModelResourceLocation modelLocation) {
        this.model = new GeometryModel(context.getModelManager().getModel(modelLocation));
    }

    @Override
    public GeometryModel getModel() {
        return model;
    }

    @Override
    public void render(TDParticle particle, PoseStack poseStack, VertexConsumer buffer, Camera camera, float partialTicks) {
        model.renderToBuffer(poseStack, buffer, particle.getLightColor(partialTicks), particle.getACol(), particle.getRCol(), particle.getGCol(), particle.getBCol());
    }
}
