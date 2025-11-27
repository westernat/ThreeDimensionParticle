package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;

@FunctionalInterface
public interface TDPRenderer {
    void render(TDParticle particle, PoseStack poseStack, VertexConsumer buffer, Camera camera, float partialTicks);

    static void doNothing(TDParticle particle, PoseStack poseStack, VertexConsumer buffer, Camera camera, float partialTicks) {}
}
