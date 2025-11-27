package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.mesdag.thr_dim_particle.client.ModelRenderer;
import org.mesdag.thr_dim_particle.client.TDParticle;
import org.mesdag.thr_dim_particle.mixed.IParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private void wrapVertexConsumer(Particle instance,
                                    VertexConsumer vertexConsumer,
                                    Camera camera,
                                    float partialTick,
                                    Operation<Void> original,
                                    @Share("customRenderTypes") LocalRef<Map<RenderType, BufferBuilder>> customRenderTypes
    ) {
        if (instance.getRenderType() == ParticleRenderType.CUSTOM) {
            ModelRenderer<?> renderer = IParticle.of(instance).tdp$getRenderer();
            if (renderer != null && renderer != ModelRenderer.DO_NOTHING) {
                RenderType renderType = renderer.getRenderType((TDParticle) instance);
                Map<RenderType, BufferBuilder> map = customRenderTypes.get();
                if (map == null) {
                    customRenderTypes.set(map = new Object2ObjectOpenHashMap<>());
                }
                vertexConsumer = map.computeIfAbsent(renderType, rt -> Tesselator.getInstance().begin(rt.mode, rt.format));
            }
        }
        original.call(instance, vertexConsumer, camera, partialTick);
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;build()Lcom/mojang/blaze3d/vertex/MeshData;"))
    private void draw(
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            Frustum frustum,
            Predicate<ParticleRenderType> renderTypePredicate,
            CallbackInfo ci,
            @Share("customRenderTypes") LocalRef<Map<RenderType, BufferBuilder>> customRenderTypes
    ) {
        Map<RenderType, BufferBuilder> map = customRenderTypes.get();
        if (map == null) return;
        for (Map.Entry<RenderType, BufferBuilder> entry : map.entrySet()) {
            MeshData meshdata = entry.getValue().build();
            if (meshdata == null) continue;
//            RenderType renderType = entry.getKey();
//            if (renderType.sortOnUpload()) {
//                ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.getOrDefault(renderType, this.sharedBuffer);
//                meshdata.sortQuads(bytebufferbuilder, RenderSystem.getVertexSorting());
//            }
            entry.getKey().draw(meshdata);
        }
    }
}
