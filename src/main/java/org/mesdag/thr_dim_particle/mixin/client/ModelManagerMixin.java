package org.mesdag.thr_dim_particle.mixin.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/AtlasSet;<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
    private Map<ResourceLocation, ResourceLocation> addCustom(Map<ResourceLocation, ResourceLocation> atlasMap) {
        return ImmutableMap.<ResourceLocation, ResourceLocation>builder()
                .putAll(atlasMap)
                .put(TDPClient.ATLAS_LOCATION, TDP.asResource("particles"))
                .build();
    }
}
