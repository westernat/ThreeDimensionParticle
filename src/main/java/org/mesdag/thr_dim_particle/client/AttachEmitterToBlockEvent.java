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
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.thr_dim_particle.client.impl.emitter.WithBlockParticleEmitter;

import java.util.Iterator;
import java.util.Map;

public class AttachEmitterToBlockEvent extends Event implements IModBusEvent {
    private static ResourceLocation defaultParticle;
    static Map<BlockState, AttachData> stateMap;
    static Map<Block, AttachData> blockMap;

    private AttachEmitterToBlockEvent() {}

    public static void postEvent() {
        defaultParticle = TDPClient.asParticle("test");
        stateMap = new Object2ObjectOpenHashMap<>();
        blockMap = new Object2ObjectOpenHashMap<>();
        ModLoader.postEvent(new AttachEmitterToBlockEvent());
        defaultParticle = null;
    }

    public AttachData attach(BlockState state, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreRange) {
        AttachData data = new AttachData.Wrapped(factory, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public AttachData attach(Block block, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreRange) {
        AttachData data = new AttachData.Wrapped(factory, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public AttachData attach(BlockState state, ResourceLocation particleId, MolangExp expression, boolean allowsVanilla, boolean ignoreRange) {
        AttachData data = new AttachData(particleId, expression, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public AttachData attach(BlockState state, ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla, boolean ignoreRange) {
        AttachData data = new AttachData(particleId, expression, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public AttachData attach(Block block, ResourceLocation particleId, MolangExp expression, boolean allowsVanilla, boolean ignoreRange) {
        AttachData data = new AttachData(particleId, expression, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public AttachData attach(Block block, ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla, boolean ignoreRange) {
        AttachData data = new AttachData(particleId, expression, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public static final Map<BlockPos, ObjectBooleanImmutablePair<WithBlockParticleEmitter>> emitters = new Object2ObjectOpenHashMap<>(64);

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
            if (emitter == null) return data.allowsVanilla;
            MolangParticleEngine.INSTANCE.addEmitter(emitter);
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

    public static class AttachData implements Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> {
        public boolean disabled = false;
        public final ResourceLocation particleId;
        public final Function3<Level, BlockPos, BlockState, MolangExp> expression;
        public final boolean ignoreSameBlock;
        public final boolean allowsVanilla;
        public final boolean ignoreRange;

        public AttachData(ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean ignoreSameBlock, boolean allowsVanilla, boolean ignoreRange) {
            this.particleId = particleId;
            this.expression = expression;
            this.ignoreSameBlock = ignoreSameBlock;
            this.allowsVanilla = allowsVanilla;
            this.ignoreRange = ignoreRange;
        }

        public AttachData(ResourceLocation particleId, MolangExp expression, boolean ignoreSameBlock, boolean allowsVanilla, boolean ignoreRange) {
            this(particleId, (level, pos, state) -> expression, ignoreSameBlock, allowsVanilla, ignoreRange);
        }

        /// Returns null means skip add emitter
        @Override
        public @Nullable WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
            if (disabled) return null;
            return new WithBlockParticleEmitter(level, pos.getCenter(), particleId, expression.apply(level, pos, state), ignoreSameBlock, ignoreRange);
        }

        static class Wrapped extends AttachData {
            private final Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory;

            Wrapped(Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreSameBlock, boolean allowsVanilla, boolean ignoreRange) {
                super(defaultParticle, MolangExp.EMPTY, ignoreSameBlock, allowsVanilla, ignoreRange);
                this.factory = factory;
            }

            @Override
            public @Nullable WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
                return factory.apply(level, pos, state);
            }
        }
    }
}
