package org.mesdag.thr_dim_particle.client.impl.emitter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public abstract class TDParticleEmitter extends ParticleEmitter {
    public final boolean ignoreRange;

    public TDParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression, boolean ignoreRange) {
        super(level, pos, particleId, expression);
        this.ignoreRange = ignoreRange;
    }

    public TDParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag);
        this.ignoreRange = tag.getBoolean("ignoreRange");
    }

    public TDParticleEmitter(ParticleEmitter parent, ParticleEffect effect, boolean ignoreRange) {
        super(parent, effect);
        this.ignoreRange = ignoreRange;
    }

    @Override
    public void tick() {
        super.tick();
        if (attachedBlock != null) {
            this.pos = attachedBlock.getBlockPos().getCenter();
        }
    }

    @Override
    public void serialize(CompoundTag compound) {
        super.serialize(compound);
        compound.putBoolean("ignoreRange", ignoreRange);
    }
}
