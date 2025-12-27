package org.mesdag.thr_dim_particle.mixin.client.effect;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.thr_dim_particle.client.ClientConfigs;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.mesdag.thr_dim_particle.client.impl.emitter.CampfireSmokeParticleEmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @ModifyExpressionValue(method = "particleTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F", ordinal = 0))
    private static float replaceParticle(
            float original,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState state,
            @Local(argsOnly = true) CampfireBlockEntity blockEntity
    ) {
        if (!level.isClientSide) return original;
        if (ClientConfigs.campfireSmoke.isEnabled()) {
            if (CampfireSmokeParticleEmitter.ableToAddCampfireEmitter(pos)) {
                ResourceLocation particle = ClientConfigs.campfireSmoke.particle;
                if (particle == null) {
                    ClientConfigs.campfireSmoke.markFailed();
                } else {
                    CampfireSmokeParticleEmitter emitter = new CampfireSmokeParticleEmitter(level, pos.getCenter(), particle);
                    emitter.attachedBlock = blockEntity;
                    PSGameClient.LOADER.addEmitter(emitter, false);
                    TDPClient.campfireEmitters.put(pos.immutable(), emitter);
                    CampfireSmokeParticleEmitter.addFireEmitter(level, pos, state.getBlock(), emitter);
                }
            }
        } else {
            CampfireSmokeParticleEmitter.addFireEmitter(level, pos, state.getBlock(), null);
            return original;
        }
        return ClientConfigs.allowsVanillaParticleWhenReachLimit ? original : 1;
    }
}
