package org.mesdag.thr_dim_particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@Mod(TDP.MODID)
public class TDP {
    public static final String MODID = "thr_dim_particle";
    public static final Logger LOGGER = LoggerFactory.getLogger("Three Dimension Particle");

    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final Supplier<ParticleType<MolangParticleOption>> TDP = ParticleStorm.registerParticleType(REGISTER, "tdp");

    public TDP(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
