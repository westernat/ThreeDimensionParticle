package org.mesdag.thr_dim_particle.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class ClientConfigs {
    private static ModConfigSpec.IntValue EMITTER_LIMIT;
    private static ModConfigSpec.IntValue FPS_THRESHOLD;
    private static ModConfigSpec.BooleanValue ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_INTERVAL_TICK;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE;
    private static ModConfigSpec.DoubleValue EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT;

    private static ModConfigSpec.BooleanValue EXPLOSION;
    private static ModConfigSpec.ConfigValue<String> EXPLOSION_PARTICLE;

    public static int emitterLimit = 50;
    public static int fpsThreshold = 30;
    public static boolean allowsVanillaParticleWhenReachLimit = false;
    public static int emitterAutoRemoveIntervalTick = 20;
    public static int emitterAutoRemoveMinimumDistance = 32;
    public static int emitterAutoRemoveAttenuationDistance = 16;
    public static double emitterAutoRemoveAttenuationCoefficient = 0.25;

    public static boolean explosion = false;
    public static @Nullable ResourceLocation explosionParticle;

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Emitter");
        EMITTER_LIMIT = builder.defineInRange("emitterLimit", 50, 20, 1000);
        FPS_THRESHOLD = builder.defineInRange("fpsThreshold", 30, 10, 260);
        ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT = builder.define("allowsVanillaParticleWhenReachLimit", false);
        EMITTER_AUTO_REMOVE_INTERVAL_TICK = builder.defineInRange("emitterAutoRemoveIntervalTick", 20, 1, 1200);
        EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE = builder.defineInRange("minimumEmitterAutoRemoveDistance", 32, 16, 256);
        EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE = builder.defineInRange("minimumEmitterAutoRemoveAttenuationDistance", 16, 0, 64);
        EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT = builder.defineInRange("minimumEmitterAutoRemoveAttenuationCoefficient", 0.25, 0, 1);
        builder.pop();

        builder.push("Particle");
        EXPLOSION = builder.define("explosion", true);
        EXPLOSION_PARTICLE = builder.define("explosionParticle", "tdp:bomb_smoke");
        builder.pop();

        container.registerConfig(ModConfig.Type.CLIENT, builder.build());
    }

    public static void onLoad() {
        emitterLimit = EMITTER_LIMIT.get();
        fpsThreshold = FPS_THRESHOLD.get();
        allowsVanillaParticleWhenReachLimit = ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT.get();
        emitterAutoRemoveIntervalTick = EMITTER_AUTO_REMOVE_INTERVAL_TICK.get();
        emitterAutoRemoveMinimumDistance = EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE.get();
        emitterAutoRemoveAttenuationDistance = EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE.get();
        emitterAutoRemoveAttenuationCoefficient = EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT.get();

        explosion = EXPLOSION.get();
        explosionParticle = ResourceLocation.tryParse(EXPLOSION_PARTICLE.get());
    }
}
