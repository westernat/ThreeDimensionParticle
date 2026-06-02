package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;
import java.util.function.Predicate;

@Mixin(value = MolangParticleEngine.class, remap = false)
public abstract class MolangParticleEngineMixin {
    @WrapOperation(method = "renderParticles", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private <T> boolean skipCheck(
            Predicate<T> instance,
            T t,
            Operation<Boolean> original,
            @Share("originalCheck") LocalBooleanRef originalCheck
    ) {
        boolean translucent = original.call(instance, t);
        if (t == TDPClient.TDP_RENDER_TYPE) {
            originalCheck.set(!translucent);
            return true; // 表示不跳过该次渲染
        }
        return translucent;
    }

    @WrapOperation(method = "renderParticles", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
    private boolean handleCustomRenderType(
            Queue<Particle> instance,
            Operation<Boolean> original,
            @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) @Nullable Frustum frustum,
            @Local(name = "type") ParticleRenderType type,
            @Share("originalCheck") LocalBooleanRef originalCheck // true为opaque，false为translucent
    ) {
        if (original.call(instance)) return true; // isEmpty
        if (frustum == null || type != TDPClient.TDP_RENDER_TYPE) return false;
        TDPClient.render(instance, camera, partialTick, frustum, originalCheck.get());
        return true; // 表示取消接下来的原版逻辑
    }
}
