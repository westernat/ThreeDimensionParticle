package org.mesdag.thr_dim_particle.client.compat.geckolib;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.mesdag.thr_dim_particle.client.ModelRenderer;
import org.mesdag.thr_dim_particle.client.TDParticle;

public class GeckolibModel {
    public GeckolibModel() {

    }

    public void renderToBuffer(TDParticle particle, Matrix4f pose, BufferBuilder buffer, float vx, float vy, float vz) {

    }

    public interface Renderer<M extends GeckolibModel> extends ModelRenderer<M> {}
}
