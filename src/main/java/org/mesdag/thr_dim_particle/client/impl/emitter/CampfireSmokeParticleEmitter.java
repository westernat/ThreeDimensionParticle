package org.mesdag.thr_dim_particle.client.impl.emitter;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.thr_dim_particle.client.ClientConfigs;
import org.mesdag.thr_dim_particle.client.TDPClient;

public class CampfireSmokeParticleEmitter extends TDParticleEmitter {
    public CampfireSmokeParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId) {
        super(level, pos, particleId, MolangExp.EMPTY, emitter -> true);
    }

    @Override
    protected void createVars() {
        super.createVars();
        Variable height = new Variable("variable.height", instance -> {
            BlockEntity blockEntity = instance.getEmitter().attachedBlock;
            if (blockEntity != null) {
                return blockEntity.getBlockState().getValue(CampfireBlock.SIGNAL_FIRE) ? 25 : 10;
            }
            return 10;
        });
        vars.table.put(height.name(), height);
    }

    @Override
    public void tick() {
        super.tick();

        children.removeIf(ParticleEmitter::isRemoved);
        if (attachedBlock != null && children.isEmpty() && !TDPClient.isFarAwayFromCamera(Minecraft.getInstance().gameRenderer.getMainCamera(), this)) {
            addFireEmitter(attachedBlock.getBlockState().getBlock(), this);
        }
    }

    public static void addFireEmitter(Block block, CampfireSmokeParticleEmitter emitter) {
        ClientConfigs.ParticleConfig fireConfig;
        if (block == Blocks.CAMPFIRE) {
            fireConfig = ClientConfigs.commonCampfireFire;
        } else if (block == Blocks.SOUL_CAMPFIRE) {
            fireConfig = ClientConfigs.soulCampfireFire;
        } else {
            return;
        }
        if (fireConfig.isEnabled()) {
            if (fireConfig.particle == null) {
                fireConfig.markFailed();
            } else {
                ParticleEffect effect = new ParticleEffect(fireConfig.particle, ParticleEffect.Type.EMITTER_BOUND, MolangExp.EMPTY);
                PSGameClient.LOADER.addEmitter(new WithBlockParticleEmitter(emitter, effect, false, e -> false), false);
            }
        }
    }
}
