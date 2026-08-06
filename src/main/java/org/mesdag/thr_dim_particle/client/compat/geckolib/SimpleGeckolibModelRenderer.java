package org.mesdag.thr_dim_particle.client.compat.geckolib;

import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4x3f;
import org.mesdag.thr_dim_particle.client.ParticleBuffer;
import org.mesdag.thr_dim_particle.client.TDPRenderType;
import org.mesdag.thr_dim_particle.client.TDParticle;
import software.bernie.geckolib.cache.GeckoLibCache;

public class SimpleGeckolibModelRenderer implements GeckolibModel.Renderer<GeckolibModel> {
    protected final GeckolibModel model;
    protected final TDPRenderType renderType;

    public SimpleGeckolibModelRenderer(
            ResourceLocation modelLocation,
            ResourceLocation textureLocation,
            TDPRenderType renderType
    ) {
        this.model = new GeckolibModel(GeckoLibCache.getBakedModels().get(modelLocation), textureLocation);
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
    public void render(TDParticle particle, Matrix4x3f pose, ParticleBuffer buffer) {
        model.renderToBuffer(particle, pose, buffer);
    }
}
