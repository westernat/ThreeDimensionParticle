package org.mesdag.thr_dim_particle.mixed;

import net.minecraft.client.particle.Particle;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.client.ModelRenderer;

public interface IParticle {
    void tdp$setRenderer(ModelRenderer<?> renderer);

    @Nullable ModelRenderer<?> tdp$getRenderer();

    static IParticle of(Particle particle) {
        return (IParticle) particle;
    }
}
