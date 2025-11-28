package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;

public interface ModelRenderer<M> {
    M getModel();

    RenderType getRenderType(TDParticle particle);

    void render(TDParticle particle, PoseStack poseStack, BufferBuilder buffer, Camera camera, float partialTick);

    ModelRenderer<?> DO_NOTHING = new ModelRenderer<>() {
        @Override
        public Object getModel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RenderType getRenderType(TDParticle particle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void render(TDParticle particle, PoseStack poseStack, BufferBuilder buffer, Camera camera, float partialTick) {}
    };
}
