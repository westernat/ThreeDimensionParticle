package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

public interface ModelRenderer<M> {
    M getModel();

    TDPRenderType getRenderType(TDParticle particle);

    void render(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz, float partialTick);

    ModelRenderer<?> DO_NOTHING = new ModelRenderer<>() {
        @Override
        public Object getModel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TDPRenderType getRenderType(TDParticle particle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void render(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz, float partialTick) {}
    };
}
