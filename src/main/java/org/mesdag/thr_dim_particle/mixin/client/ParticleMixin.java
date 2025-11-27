package org.mesdag.thr_dim_particle.mixin.client;

import net.minecraft.client.particle.Particle;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.client.ModelRenderer;
import org.mesdag.thr_dim_particle.mixed.IParticle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Particle.class)
public abstract class ParticleMixin implements IParticle {
    @Override
    public void tdp$setRenderer(ModelRenderer<?> renderer) {}

    @Override
    public @Nullable ModelRenderer<?> tdp$getRenderer() {
        return null;
    }
}
