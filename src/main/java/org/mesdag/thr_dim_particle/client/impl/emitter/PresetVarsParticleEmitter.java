package org.mesdag.thr_dim_particle.client.impl.emitter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

public class PresetVarsParticleEmitter extends TDParticleEmitter {
    private @Nullable Runnable createVarsCallback;

    public PresetVarsParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, boolean ignoreRange, Variable... variables) {
        super(level, pos, particleId, MolangExp.EMPTY, ignoreRange);
        this.createVarsCallback = () -> {
            for (Variable var : variables) {
                vars.table.put(var.name(), var);
            }
        };
        super.init();
    }

    @Override
    protected void init() {}

    @Override
    protected void createVars() {
        super.createVars();
        if (createVarsCallback != null) {
            createVarsCallback.run();
            this.createVarsCallback = null;
        }
    }
}
