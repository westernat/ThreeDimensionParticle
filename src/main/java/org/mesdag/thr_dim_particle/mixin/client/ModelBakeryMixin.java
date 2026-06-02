package org.mesdag.thr_dim_particle.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.thr_dim_particle.client.RegisterTDPRendererEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> unbakedCache;

    @Shadow
    protected abstract BlockModel loadBlockModel(ResourceLocation location) throws IOException;

    @Shadow
    @Final
    private Set<ResourceLocation> loadingStack;

    @Inject(method = "loadModel(Lnet/minecraft/resources/ResourceLocation;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER), cancellable = true)
    private void checkStandalone(ResourceLocation rl, CallbackInfo ci, @Local(name = "modelresourcelocation") ModelResourceLocation modelRl) throws IOException {
        if (RegisterTDPRendererEvent.TDP_VARTIANT.equals(modelRl.getVariant())) {
            UnbakedModel unbakedmodel = loadBlockModel(rl);
            unbakedCache.put(rl, unbakedmodel);
            loadingStack.addAll(unbakedmodel.getDependencies());
            ci.cancel();
        }
    }
}
