package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalByteRef;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;
import java.util.function.Predicate;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private <T> boolean skipCheck(Predicate<T> instance, T t, Operation<Boolean> original, @Share("originalCheck") LocalByteRef originalCheck) {
        boolean called = original.call(instance, t);
        if (t == TDPClient.TDP_RENDER_TYPE) {
            originalCheck.set((byte) (called ? 2 : 1)); // 2为translucent，1为opaque
            return true;
        }
        return called;
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
    private boolean handleCustomRenderType(
            Queue<Particle> instance,
            Operation<Boolean> original,
            @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) @Nullable Frustum frustum,
            @Share("originalCheck") LocalByteRef originalCheck
    ) {
        if (original.call(instance)) return true; // isEmpty
        byte b = originalCheck.get();
        if (b == 0 || frustum == null) return false;
        originalCheck.set((byte) 0);
        TDPClient.render(instance, camera, partialTick, frustum, b == 1);
        return true; // 表示取消接下来的原版逻辑
    }
}
