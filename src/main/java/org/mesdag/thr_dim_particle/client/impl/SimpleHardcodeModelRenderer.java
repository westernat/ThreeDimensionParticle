package org.mesdag.thr_dim_particle.client.impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.mesdag.thr_dim_particle.client.HardcodeModel;
import org.mesdag.thr_dim_particle.client.TDPRenderType;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class SimpleHardcodeModelRenderer implements HardcodeModel.Renderer<HardcodeModel> {
    protected final HardcodeModel model;

    public SimpleHardcodeModelRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation layerLocation,
            ResourceLocation textureLocation,
            TDPRenderType renderType) {
        this.model = new HardcodeModel(context.getModelSet().bakeLayer(layerLocation), textureLocation, renderType);
    }

    @Override
    public HardcodeModel getModel() {
        return model;
    }

    @Override
    public void render(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz) {
        model.renderToBuffer(particle, pose, buffer, vx, vy, vz);
    }
}
