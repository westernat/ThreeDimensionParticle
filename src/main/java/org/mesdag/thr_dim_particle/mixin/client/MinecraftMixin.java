package org.mesdag.thr_dim_particle.mixin.client;

import net.minecraft.client.Minecraft;
import org.mesdag.thr_dim_particle.client.RegisterTDPRendererEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;",at=@At("HEAD"))
    private void start(boolean error, @Coerce Object gameLoadCookie, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        RegisterTDPRendererEvent.start();
    }

    @Inject(method = "onResourceLoadFinished", at=@At("TAIL"))
    private void end(@Coerce Object gameLoadCookie, CallbackInfo ci) {
        RegisterTDPRendererEvent.end();
    }
}
