package org.mesdag.thr_dim_particle.client.impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.mesdag.thr_dim_particle.client.HardcodeModel;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class SimpleHardcodeModelRenderer implements HardcodeModel.Renderer<HardcodeModel> {
    protected final HardcodeModel model;
    protected final ResourceLocation textureLocation;

    public SimpleHardcodeModelRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation layerLocation,
            ResourceLocation textureLocation
    ) {
        this.textureLocation = textureLocation;
        this.model = new HardcodeModel(context.getModelSet().bakeLayer(layerLocation));
    }

    @Override
    public HardcodeModel getModel() {
        return model;
    }

    @Override
    public ResourceLocation getTextureLocation(TDParticle particle) {
        return textureLocation;
    }

    @Override
    public void render(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz) {
        model.renderToBuffer(pose, buffer, particle.getLightColor(0), OverlayTexture.NO_OVERLAY, particle.argb);
    }
}
