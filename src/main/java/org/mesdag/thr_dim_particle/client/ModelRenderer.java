package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public interface ModelRenderer<M> extends TDPRenderer {
    M getModel();

    RenderType getRenderType(TDParticle particle);

    @Override
    default void render(TDParticle particle, PoseStack poseStack, VertexConsumer buffer, Camera camera, float partialTicks) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        buffer = bufferSource.getBuffer(getRenderType(particle));
        actuallyRender(particle, buffer, poseStack, partialTicks);
        bufferSource.endBatch();
    }

    void actuallyRender(TDParticle particle, VertexConsumer buffer, PoseStack poseStack, float partialTicks);
}
