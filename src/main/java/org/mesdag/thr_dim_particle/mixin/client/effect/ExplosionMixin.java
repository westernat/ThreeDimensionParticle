package org.mesdag.thr_dim_particle.mixin.client.effect;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.thr_dim_particle.client.ClientConfigs;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double y;

    @Shadow
    @Final
    private double z;

    @Definition(id = "spawnParticles", local = @Local(type = boolean.class, ordinal = 0, argsOnly = true))
    @Expression("spawnParticles")
    @ModifyExpressionValue(method = "finalizeExplosion", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean takeOverParticle(boolean original) {
        if (original && ClientConfigs.EXPLOSION.get()) {
            PSGameClient.LOADER.addEmitter(new ParticleEmitter(level, new Vec3(x, y, z), TDPClient.EXPLOSION_PARTICLE), false);
            return false;
        }
        return original;
    }
}
