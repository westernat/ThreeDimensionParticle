package org.mesdag.thr_dim_particle.mixin.sodium;

import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.mesdag.thr_dim_particle.client.compat.sodium.SodiumTickerOptimizationIgnorer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AnimationMetadataSection.class)
public class AnimationMetadataSectionMixin implements SodiumTickerOptimizationIgnorer {
    @Unique
    private boolean tdp$ignored;

    @Override
    public void tdp$setIgnored() {
        this.tdp$ignored = true;
    }

    @Override
    public boolean tdp$isIgnored() {
        return tdp$ignored;
    }
}
