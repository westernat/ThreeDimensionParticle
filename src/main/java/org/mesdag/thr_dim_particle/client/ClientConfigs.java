package org.mesdag.thr_dim_particle.client;

import it.unimi.dsi.fastutil.objects.ObjectBooleanImmutablePair;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.impl.emitter.TDParticleEmitter;
import org.mesdag.thr_dim_particle.client.impl.emitter.WithBlockParticleEmitter;

import java.util.Iterator;
import java.util.List;

public final class ClientConfigs {
    private static ModConfigSpec.IntValue EMITTER_LIMIT;
    private static ModConfigSpec.IntValue FPS_THRESHOLD;
    private static ModConfigSpec.BooleanValue ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_INTERVAL_TICK;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE;
    private static ModConfigSpec.DoubleValue EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT;

    public static int emitterLimit = 50;
    public static int fpsThreshold = 30;
    public static boolean allowsVanillaParticleWhenReachLimit = false;
    public static int emitterAutoRemoveIntervalTick = 1;
    public static int emitterAutoRemoveMinimumDistance = 32;
    public static int emitterAutoRemoveAttenuationDistance = 16;
    public static double emitterAutoRemoveAttenuationCoefficient = 0.25;

    public static ParticleConfig explosion;
    public static ParticleConfig endRod;
    public static ParticleConfig netherPortal;
    public static ParticleConfig commonCampfireFire;
    public static ParticleConfig soulCampfireFire;
    public static ParticleConfig campfireSmoke;

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Emitter");
        EMITTER_LIMIT = builder.defineInRange("emitterLimit", 50, 20, 1000);
        FPS_THRESHOLD = builder.defineInRange("fpsThreshold", 30, 10, 260);
        ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT = builder.define("allowsVanillaParticleWhenReachLimit", false);
        EMITTER_AUTO_REMOVE_INTERVAL_TICK = builder.defineInRange("emitterAutoRemoveIntervalTick", 1, 1, 1200);
        EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE = builder.defineInRange("minimumEmitterAutoRemoveDistance", 32, 16, 256);
        EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE = builder.defineInRange("minimumEmitterAutoRemoveAttenuationDistance", 16, 0, 64);
        EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT = builder.defineInRange("minimumEmitterAutoRemoveAttenuationCoefficient", 0.25, 0, 1);
        builder.pop();

        builder.push("Particle");
        explosion = new ParticleConfig(builder, "explosion", "bomb_smoke");
        endRod = new ParticleConfig(builder, "endRod", "end_rod");
        netherPortal = new ParticleConfig(builder, "netherPortal", "nether_portal");
        Runnable campfireCallback = () -> {
            Iterator<TDParticleEmitter> iterator = TDPClient.campfireEmitters.values().iterator();
            while (iterator.hasNext()) {
                TDParticleEmitter emitter = iterator.next();
                emitter.remove();
                iterator.remove();
            }
        };
        commonCampfireFire = new ParticleConfig(builder, "commonCampfireFire", "fire", campfireCallback);
        soulCampfireFire = new ParticleConfig(builder, "soulCampfireFire", "soul_fire", campfireCallback);
        campfireSmoke = new ParticleConfig(builder, "campfireSmoke", "campfire_smoke", campfireCallback);
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

        explosion.onLoad();
        endRod.onLoad();
        netherPortal.onLoad();
        commonCampfireFire.onLoad();
        soulCampfireFire.onLoad();
        campfireSmoke.onLoad();
    }

    public static class ParticleConfig {
        private boolean enabled = true;
        public @Nullable ResourceLocation particle;
        private @Nullable List<AttachEmitterToBlockEvent.AttachData> associated;
        public boolean failed = false;

        private final String configPath;
        private final @Nullable Runnable onLoadCallback;
        private final ModConfigSpec.BooleanValue ENABLE;
        private final ModConfigSpec.ConfigValue<String> PARTICLE;

        public ParticleConfig(ModConfigSpec.Builder builder, String configPath, String particlePath) {
            this(builder, configPath, particlePath, null);
        }

        public ParticleConfig(ModConfigSpec.Builder builder, String configPath, String particlePath, @Nullable Runnable onLoadCallback) {
            this.configPath = configPath;
            this.onLoadCallback = onLoadCallback;
            this.ENABLE = builder.define(configPath, true);
            this.PARTICLE = builder.define(configPath + "Particle", "tdp:" + particlePath);
        }

        public boolean isEnabled() {
            return enabled && !failed;
        }

        public void onLoad() {
            this.enabled = ENABLE.get();
            this.particle = ResourceLocation.tryParse(PARTICLE.get());
            updateAssociated();
            if (onLoadCallback != null) {
                Minecraft.getInstance().execute(onLoadCallback);
            }
        }

        public void initAssociated(List<AttachEmitterToBlockEvent.AttachData> associated) {
            this.associated = associated;
            updateAssociated();
        }

        private void updateAssociated() {
            if (associated == null) return;
            boolean disabled = !enabled;
            for (AttachEmitterToBlockEvent.AttachData data : associated) {
                data.disabled = disabled;
            }
            if (particle == null) return;
            Minecraft.getInstance().execute(() -> {
                for (ObjectBooleanImmutablePair<WithBlockParticleEmitter> pair : AttachEmitterToBlockEvent.emitters.values()) {
                    WithBlockParticleEmitter emitter = pair.left();
                    if (particle.equals(emitter.particleId)) {
                        emitter.remove();
                    }
                }
            });
        }

        public void markFailed() {
            this.failed = true;
            TDP.LOGGER.warn("Error get {} particle", configPath);
        }

        @Override
        public String toString() {
            return "ParticleConfig{" +
                    "configPath='" + configPath + '\'' +
                    '}';
        }
    }
}
