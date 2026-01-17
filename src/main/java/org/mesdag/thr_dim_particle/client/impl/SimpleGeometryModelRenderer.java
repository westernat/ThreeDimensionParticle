package org.mesdag.thr_dim_particle.client.impl;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.joml.Matrix4f;
import org.mesdag.thr_dim_particle.client.GeometryModel;
import org.mesdag.thr_dim_particle.client.ParticleBuffer;
import org.mesdag.thr_dim_particle.client.TDPRenderType;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class SimpleGeometryModelRenderer implements GeometryModel.Renderer<GeometryModel> {
    protected final GeometryModel model;
    protected final TDPRenderType renderType;

    public SimpleGeometryModelRenderer(EntityRendererProvider.Context context, ModelResourceLocation modelLocation, TDPRenderType renderType) {
        this.model = new GeometryModel(context.getModelManager().getModel(modelLocation));
        this.renderType = renderType;
    }

    @Override
    public GeometryModel getModel() {
        return model;
    }

    @Override
    public TDPRenderType getRenderType() {
        return renderType;
    }

    @Override
    public void render(TDParticle particle, Matrix4f pose, ParticleBuffer buffer) {
        model.renderToBuffer(particle, pose, buffer);
    }
}
