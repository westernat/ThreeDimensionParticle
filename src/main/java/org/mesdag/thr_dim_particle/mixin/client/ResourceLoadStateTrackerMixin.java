package org.mesdag.thr_dim_particle.mixin.client;

import net.minecraft.client.ResourceLoadStateTracker;
import org.mesdag.thr_dim_particle.client.RegisterTDPRendererEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public abstract class ResourceLoadStateTrackerMixin {
    @Inject(method = "finishReload", at = @At("TAIL"))
    private void end(CallbackInfo ci) {
        RegisterTDPRendererEvent.end();
    }
}
