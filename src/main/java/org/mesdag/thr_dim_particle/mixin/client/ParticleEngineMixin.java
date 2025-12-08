package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z"))
    private boolean handleCustomRenderType(
            Queue<Particle> instance,
            Operation<Boolean> original,
            @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) @Nullable Frustum frustum,
            @Local ParticleRenderType particleRenderType
    ) {
        if (original.call(instance)) return true; // isEmpty
        if (frustum != null && particleRenderType == TDPClient.TDP_RENDER_TYPE) {
            TDPClient.render(instance, camera, partialTick, frustum);
            return true; // 表示取消接下来的原版逻辑
        }
        return false;
    }

//    @Shadow
//    protected ClientLevel level;
//
//    @Shadow
//    protected abstract void tickParticleList(Collection<Particle> particles);
//
//    @Unique
//    private final Map<Section, Queue<TDParticle>> tdp$particles = new Object2ObjectOpenHashMap<>();
//
//    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V"))
//    private void handle(LightTexture lightTexture, Camera camera, float partialTick, @Nullable Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate, CallbackInfo ci) {
//        if (frustum == null || !renderTypePredicate.test(TDPClient.TDP_RENDER_TYPE)) return;
//        for (Map.Entry<Section, Queue<TDParticle>> entry : tdp$particles.entrySet()) {
//            if (!frustum.isVisible(entry.getKey().getBox())) continue;
//            Queue<TDParticle> queue = entry.getValue();
//            if (queue == null || queue.isEmpty()) continue;
//            TDPClient.render(queue, camera, partialTick, frustum);
//        }
//    }
//
//    @SuppressWarnings({"unchecked", "rawtypes"})
//    @Inject(method = "tick", at = @At("HEAD"))
//    private void tickTdp(CallbackInfo ci) {
//        tdp$particles.forEach(((section, tdp) -> {
//            level.getProfiler().push(section.toString());
//            tickParticleList((Queue<Particle>) (Queue) tdp);
//            level.getProfiler().pop();
//        }));
//    }
//
//    @SuppressWarnings("unchecked")
//    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
//    private <K, V> V relink(Map<K, V> instance, K key, Function<? super K, ? extends V> function, Operation<V> original, @Local Particle particle) {
//        if (particle.getRenderType() == TDPClient.TDP_RENDER_TYPE) {
//            TDParticle tdp = (TDParticle) particle;
//            return (V) tdp$particles.computeIfAbsent(Section.of(tdp.getX(), tdp.getY(), tdp.getZ()), section -> {
//                section.getBox();
//                return EvictingQueue.create(16384);
//            });
//        }
//        return original.call(instance, key, function);
//    }
//
//    @Inject(method = "clearParticles", at = @At("TAIL"))
//    private void clear(CallbackInfo ci) {
//        tdp$particles.clear();
//    }
}
