package org.mesdag.thr_dim_particle.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfigs {
    public static ModConfigSpec.BooleanValue EXPLOSION;

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        EXPLOSION = builder.define("explosion", true);

        container.registerConfig(ModConfig.Type.CLIENT, builder.build());
    }
}
