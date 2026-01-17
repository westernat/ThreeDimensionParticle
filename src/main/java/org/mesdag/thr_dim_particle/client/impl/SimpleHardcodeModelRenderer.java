package org.mesdag.thr_dim_particle.client.impl;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.mesdag.thr_dim_particle.client.HardcodeModel;
import org.mesdag.thr_dim_particle.client.ParticleBuffer;
import org.mesdag.thr_dim_particle.client.TDPRenderType;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class SimpleHardcodeModelRenderer implements HardcodeModel.Renderer<HardcodeModel> {
    protected final HardcodeModel model;
    protected final TDPRenderType renderType;

    public SimpleHardcodeModelRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation layerLocation,
            ResourceLocation textureLocation,
            TDPRenderType renderType
    ) {
        this.model = new HardcodeModel(context.getModelSet().bakeLayer(layerLocation), textureLocation);
        this.renderType = renderType;
    }

    @Override
    public HardcodeModel getModel() {
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
