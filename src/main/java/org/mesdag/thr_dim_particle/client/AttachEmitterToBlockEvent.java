package org.mesdag.thr_dim_particle.client;

import com.mojang.datafixers.util.Function3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBooleanImmutablePair;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.thr_dim_particle.client.impl.WithBlockParticleEmitter;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;

public class AttachEmitterToBlockEvent extends Event implements IModBusEvent {
    private static ResourceLocation defaultParticle;
    private static Map<BlockState, AttachData> stateMap;
    private static Map<Block, AttachData> blockMap;

    private AttachEmitterToBlockEvent() {}

    public static void postEvent() {
        defaultParticle = ResourceLocation.fromNamespaceAndPath("tdp", "test");
        stateMap = new Object2ObjectOpenHashMap<>();
        blockMap = new Object2ObjectOpenHashMap<>();
        ModLoader.postEvent(new AttachEmitterToBlockEvent());
        defaultParticle = null;
    }

    public void attach(BlockState state, boolean allowsVanilla, BiFunction<Level, BlockPos, WithBlockParticleEmitter> factory) {
        stateMap.put(state, new AttachData(defaultParticle, MolangExp.EMPTY, false, allowsVanilla) {
            @Override
            public WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
                return factory.apply(level, pos);
            }
        });
    }

    public void attach(Block block, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, WithBlockParticleEmitter> factory) {
        blockMap.put(block, new AttachData(defaultParticle, MolangExp.EMPTY, true, allowsVanilla) {
            @Override
            public WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
                return factory.apply(level, pos, state);
            }
        });
    }

    public void attach(BlockState state, ResourceLocation particleId, MolangExp expression, boolean allowsVanilla) {
        stateMap.put(state, new AttachData(particleId, expression, false, allowsVanilla));
    }

    public void attach(BlockState state, ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla) {
        stateMap.put(state, new AttachData(particleId, expression, false, allowsVanilla));
    }

    public void attach(Block block, ResourceLocation particleId, MolangExp expression, boolean allowsVanilla) {
        blockMap.put(block, new AttachData(particleId, expression, true, allowsVanilla));
    }

    public void attach(Block block, ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla) {
        blockMap.put(block, new AttachData(particleId, expression, true, allowsVanilla));
    }

    static final Map<BlockPos, ObjectBooleanImmutablePair<WithBlockParticleEmitter>> emitters = new Object2ObjectOpenHashMap<>();

    public static boolean attachTo(Block block, BlockState state, Level level, BlockPos pos) {
        if (!TDPClient.ableToAddEmitter()) {
            ObjectBooleanImmutablePair<WithBlockParticleEmitter> pair = emitters.remove(pos);
            if (pair != null) {
                pair.left().remove();
            }
            return ClientConfigs.allowsVanillaParticleWhenReachLimit;
        }
        ObjectBooleanImmutablePair<WithBlockParticleEmitter> pair = emitters.get(pos);
        if (pair == null) {
            AttachData data = blockMap.get(block);
            if (data == null && (data = stateMap.get(state)) == null) return true;
            WithBlockParticleEmitter emitter = data.apply(level, pos, state);
            PSGameClient.LOADER.addEmitter(emitter, false);
            emitters.put(pos.immutable(), pair = new ObjectBooleanImmutablePair<>(emitter, data.allowsVanilla));
        }
        return pair.rightBoolean();
    }

    public static void tick(Camera camera) {
        if (emitters.isEmpty()) return;
        Iterator<ObjectBooleanImmutablePair<WithBlockParticleEmitter>> iterator = emitters.values().iterator();
        while (iterator.hasNext()) {
            WithBlockParticleEmitter emitter = iterator.next().left();
            if (TDPClient.shouldRemoveEmitter(camera, emitter)) {
                emitter.remove();
                iterator.remove();
            }
        }
    }

    public static void clearEmitters() {
        emitters.clear();
    }

    public static class AttachData implements Function3<Level, BlockPos, BlockState, WithBlockParticleEmitter> {
        public final ResourceLocation particleId;
        public final Function3<Level, BlockPos, BlockState, MolangExp> expression;
        public final boolean ignoreSameBlock;
        public final boolean allowsVanilla;

        public AttachData(ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean ignoreSameBlock, boolean allowsVanilla) {
            this.particleId = particleId;
            this.expression = expression;
            this.ignoreSameBlock = ignoreSameBlock;
            this.allowsVanilla = allowsVanilla;
        }

        public AttachData(ResourceLocation particleId, MolangExp expression, boolean ignoreSameBlock, boolean allowsVanilla) {
            this(particleId, (level, pos, state) -> expression, ignoreSameBlock, allowsVanilla);
        }

        @Override
        public WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
            return new WithBlockParticleEmitter(level, pos.getCenter(), particleId, expression.apply(level, pos, state), ignoreSameBlock);
        }
    }
}
