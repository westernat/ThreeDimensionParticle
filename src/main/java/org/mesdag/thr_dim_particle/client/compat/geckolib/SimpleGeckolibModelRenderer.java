package org.mesdag.thr_dim_particle.client.compat.geckolib;

import org.joml.Matrix4f;
import org.mesdag.thr_dim_particle.client.ParticleBuffer;
import org.mesdag.thr_dim_particle.client.TDPRenderType;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class SimpleGeckolibModelRenderer implements GeckolibModel.Renderer<GeckolibModel> {
    protected final GeckolibModel model;
    protected final TDPRenderType renderType;

    public SimpleGeckolibModelRenderer(GeckolibModel model, TDPRenderType renderType) {
        this.model = model;
        this.renderType = renderType;
    }

    @Override
    public GeckolibModel getModel() {
        return model;
    }

    @Override
    public TDPRenderType getRenderType() {
        return renderType;
    }

    @Override
    public void render(TDParticle particle, Matrix4f pose, ParticleBuffer buffer, float vx, float vy, float vz) {
        model.renderToBuffer(particle, pose, buffer, vx, vy, vz);
    }
}
