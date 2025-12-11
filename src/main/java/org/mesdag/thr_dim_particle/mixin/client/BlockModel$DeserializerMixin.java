package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockModel.Deserializer.class)
public abstract class BlockModel$DeserializerMixin {
    @WrapOperation(method = "parseTextureLocationOrReference", at = @At(value = "NEW", target = "(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/Material;"))
    private static Material modifyAtlas(ResourceLocation atlasLocation, ResourceLocation texture, Operation<Material> original) {
        if (texture.getPath().startsWith("tdp/")) {
            return original.call(TDPClient.ATLAS_LOCATION, texture);
        }
        return original.call(atlasLocation, texture);
    }
}
