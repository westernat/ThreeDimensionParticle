package org.mesdag.thr_dim_particle.mixin.sodium;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import org.mesdag.thr_dim_particle.client.compat.sodium.SodiumTickerOptimizationIgnorer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnimationMetadataSectionSerializer.class)
public abstract class AnimationMetadataSectionSerializerMixin {
    @ModifyReturnValue(method = "fromJson(Lcom/google/gson/JsonObject;)Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;", at = @At("RETURN"))
    private AnimationMetadataSection readAdditional(AnimationMetadataSection original, @Local(argsOnly = true) JsonObject json) {
        if (GsonHelper.getAsBoolean(json, "tdp_ignore_sodium_ticker_optimization", false)) {
            SodiumTickerOptimizationIgnorer.setIgnore(original);
        }
        return original;
    }
}
