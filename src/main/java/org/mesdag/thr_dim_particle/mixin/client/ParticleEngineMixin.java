package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.client.ModelRenderer;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.mesdag.thr_dim_particle.client.TDParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
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
            Map<RenderType, BufferBuilder> map = new Object2ObjectOpenHashMap<>();
            for (Particle particle : instance) {
                if (!frustum.isVisible(particle.getRenderBoundingBox(partialTick))) continue;
                ModelRenderer<?> renderer = ((TDParticle) particle).renderer;
                if (renderer == ModelRenderer.DO_NOTHING) continue;
                try {
                    particle.render(map.computeIfAbsent(renderer.getRenderType((TDParticle) particle),
                            rt -> Tesselator.getInstance().begin(rt.mode, rt.format)
                    ), camera, partialTick);
                } catch (Throwable throwable) {
                    CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                    CrashReportCategory category = report.addCategory("Particle being rendered");
                    category.setDetail("Particle", particle::toString);
                    category.setDetail("Particle Type", particleRenderType::toString);
                    throw new ReportedException(report);
                }
            }

            for (Map.Entry<RenderType, BufferBuilder> entry : map.entrySet()) {
                MeshData data = entry.getValue().build();
                if (data == null) continue;
                entry.getKey().draw(data);
            }
            return true; // 表示取消接下来的原版逻辑
        }
        return false;
    }
}
