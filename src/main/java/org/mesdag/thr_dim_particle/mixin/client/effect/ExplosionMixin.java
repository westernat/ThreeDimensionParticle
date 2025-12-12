package org.mesdag.thr_dim_particle.mixin.client.effect;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.ClientConfigs;
import org.mesdag.thr_dim_particle.client.TDPClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static boolean tdp$success = true;

    @Definition(id = "spawnParticles", local = @Local(type = boolean.class, ordinal = 0, argsOnly = true))
    @Expression("spawnParticles")
    @ModifyExpressionValue(method = "finalizeExplosion", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean takeOverParticle(boolean original) {
        if (original && tdp$success && ClientConfigs.explosion) {
            if (ClientConfigs.explosionParticle == null) {
                tdp$success = false;
                TDP.errorGetParticle("explosion");
                return true;
            }
            TDPClient.addEmitter(level, new Vec3(x, y, z), ClientConfigs.explosionParticle);
            return false;
        }
        return original;
    }
}
