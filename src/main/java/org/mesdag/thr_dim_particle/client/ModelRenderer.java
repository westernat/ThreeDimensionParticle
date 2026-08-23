package org.mesdag.thr_dim_particle.client;

import org.joml.Matrix4x3f;

public interface ModelRenderer<M> {
    M getModel();

    TDPRenderType getRenderType();

    void render(TDParticle particle, Matrix4x3f pose, ParticleBuffer buffer);

    ModelRenderer<?> DO_NOTHING = new ModelRenderer<>() {
        @Override
        public Object getModel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TDPRenderType getRenderType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void render(TDParticle particle, Matrix4x3f pose, ParticleBuffer buffer) {}
    };
}
