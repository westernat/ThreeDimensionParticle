package org.mesdag.thr_dim_particle.mixin.sodium;

import com.mojang.blaze3d.platform.NativeImage;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.client.compat.sodium.SodiumTickerOptimizationIgnorer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SodiumTickerOptimizationIgnorer {
    @Unique
    private boolean tdp$ignored;

    @Override
    public void tdp$setIgnored() {
        this.tdp$ignored = true;
    }

    @Override
    public boolean tdp$isIgnored() {
        return tdp$ignored;
    }

    @Inject(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;Lnet/minecraftforge/client/textures/ForgeTextureMetadata;)V", at = @At("TAIL"), remap = false)
    private void save(ResourceLocation name, FrameSize frameSize, NativeImage originalImage, AnimationMetadataSection metadata, ForgeTextureMetadata forgeMeta, CallbackInfo ci) {
        if (SodiumTickerOptimizationIgnorer.isIgnored(metadata)) {
            tdp$setIgnored();
        }
    }

    /// @see me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking.SpriteContentsAnimatorImplMixin
    @Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker", priority = 900)
    public static class TickerMixin {
        @Unique
        private @Nullable SpriteContents tdp$parent;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void save(SpriteContents this$0, @Coerce Object animationInfo, @Coerce Object interpolationData, CallbackInfo ci) {
            if (SodiumTickerOptimizationIgnorer.isIgnored(this$0)) {
                this.tdp$parent = this$0;
            }
        }

        @Inject(method = "tickAndUpload", at = @At("HEAD"))
        private void preActive(int x, int y, CallbackInfo ci) {
            if (tdp$parent != null) {
                ((SpriteContentsExtended) tdp$parent).sodium$setActive(true);
            }
        }
    }
}
