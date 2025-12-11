package org.mesdag.thr_dim_particle.client.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public class WithBlockParticleEmitter extends ParticleEmitter {
    protected @Nullable WithBlockParticleEmitter.BlockData blockData;

    public WithBlockParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression, boolean ignoreSameBlock) {
        super(level, pos, particleId, expression);
        initBlock(level, pos, ignoreSameBlock);
    }

    public WithBlockParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag);
        initBlock(level, pos, tag.getBoolean("ignoreSameBlock"));
    }

    private void initBlock(Level level, Vec3 pos, boolean ignoreSameBlock) {
        BlockPos blockPos = BlockPos.containing(pos);
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) {
            remove();
        } else {
            this.blockData = new BlockData(blockPos, state, ignoreSameBlock);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (blockData != null) {
            BlockState state = level.getBlockState(blockData.pos);
            if (state != blockData.state && (!blockData.ignoreSameBlock || state.getBlock() != blockData.state.getBlock())) {
                remove();
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return super.isRemoved() || blockData == null;
    }

    @Override
    public void serialize(CompoundTag compound) {
        super.serialize(compound);
        compound.putString(RegisterCustomEmitterTypeEvent.TYPE_KEY, "thr_dim_particle:type");
        if (blockData != null) {
            compound.putBoolean("ignoreSameBlock", blockData.ignoreSameBlock);
        }
    }

    public record BlockData(BlockPos pos, BlockState state, boolean ignoreSameBlock) {}
}
