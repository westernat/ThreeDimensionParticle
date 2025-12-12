package org.mesdag.thr_dim_particle.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class ClientConfigs {
    private static ModConfigSpec.IntValue EMITTER_LIMIT;
    private static ModConfigSpec.IntValue FPS_THRESHOLD;
    private static ModConfigSpec.BooleanValue EXPLOSION;
    private static ModConfigSpec.ConfigValue<String> EXPLOSION_PARTICLE;

    public static int emitterLimit = 50;
    public static int fpsThreshold = 30;
    public static boolean explosion = false;
    public static @Nullable ResourceLocation explosionParticle;

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        EMITTER_LIMIT = builder.defineInRange("emitterLimit", 50, 20, 1000);
        FPS_THRESHOLD = builder.defineInRange("fpsThreshold", 30, 10, 260);
        EXPLOSION = builder.define("explosion", true);
        EXPLOSION_PARTICLE = builder.define("explosionParticle", "tdp:bomb_smoke");

        container.registerConfig(ModConfig.Type.CLIENT, builder.build());
    }

    public static void onLoad() {
        emitterLimit = EMITTER_LIMIT.get();
        fpsThreshold = FPS_THRESHOLD.get();
        explosion = EXPLOSION.get();
        explosionParticle = ResourceLocation.tryParse(EXPLOSION_PARTICLE.get());
    }
}
