package org.mesdag.thr_dim_particle.client.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.thr_dim_particle.client.HardcodeModel;
import org.mesdag.thr_dim_particle.client.TDParticle;

import java.util.function.Function;

public class SimpleHardcodeModelRenderer implements HardcodeModel.Renderer<HardcodeModel> {
    protected final HardcodeModel model;
    protected final ResourceLocation textureLocation;

    public SimpleHardcodeModelRenderer(
            Function<ResourceLocation, RenderType> renderType,
            EntityRendererProvider.Context context,
            ModelLayerLocation layerLocation,
            ResourceLocation textureLocation
    ) {
        this.textureLocation = textureLocation;
        this.model = new HardcodeModel(renderType, context.getModelSet().bakeLayer(layerLocation));
    }

    public SimpleHardcodeModelRenderer(
            EntityRendererProvider.Context context,
            ModelLayerLocation layerLocation,
            ResourceLocation textureLocation
    ) {
        this(RenderType::entityCutout, context, layerLocation, textureLocation);
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
    public void render(TDParticle particle, PoseStack poseStack, VertexConsumer buffer, Camera camera, float partialTicks) {
        model.renderToBuffer(poseStack, buffer, particle.getLightColor(partialTicks), OverlayTexture.NO_OVERLAY, particle.argb);
    }
}
