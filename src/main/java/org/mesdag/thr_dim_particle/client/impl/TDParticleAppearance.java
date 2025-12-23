package org.mesdag.thr_dim_particle.client.impl;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.DuplicateFieldDecoder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.ModelRenderer;
import org.mesdag.thr_dim_particle.client.RegisterTDPRendererEvent;
import org.mesdag.thr_dim_particle.client.TDParticle;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.Direction;
import static org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.FaceCameraMode;

public record TDParticleAppearance(
        FloatMolangExp3 size,
        Optional<FaceCameraMode> faceCameraMode,
        Direction direction,
        ModelAnimation modelAnimation
) implements IParticleComponent {
    public static final ResourceLocation ID = TDP.asResource("appearance");
    public static final Codec<TDParticleAppearance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp3.CODEC.fieldOf("size").forGetter(TDParticleAppearance::size),
            DuplicateFieldDecoder.optionalFieldOf(FaceCameraMode.CODEC, "face_camera_mode", "facing_camera_mode").forGetter(TDParticleAppearance::faceCameraMode),
            Direction.CODEC.lenientOptionalFieldOf("direction", Direction.DEFAULT).forGetter(TDParticleAppearance::direction),
            ModelAnimation.CODEC.lenientOptionalFieldOf("model_animation", ModelAnimation.EMPTY).forGetter(TDParticleAppearance::modelAnimation)
    ).apply(instance, TDParticleAppearance::new));

    @Override
    public Codec<TDParticleAppearance> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                size.exp1(), size.exp2(), size.exp3(),
                direction.customDirection().exp1(), direction.customDirection().exp2(), direction.customDirection().exp3()
        );
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        TDParticle particle = (TDParticle) instance;
        doInit(particle);
        int maxFrame = particle.getMaxFrame();
        if (maxFrame <= 0) return;
        if (modelAnimation.stretchToLifetime) {
            modelAnimation.setCurrentModel(particle);
            int frame = maxFrame * particle.getAge() / particle.getLifetime();
            particle.setCurrentFrame(frame == maxFrame ? maxFrame - 1 : frame);
            return;
        }
        float gameTime = (float) ((int) particle.getLevel().getGameTime() & 0b11111111);
        if (gameTime % (particle.getLevel().tickRateManager().tickrate() / modelAnimation.framesPerSecond) < 1.0F) {
            modelAnimation.setCurrentModel(particle);
            int currentFrame = particle.getCurrentFrame() + 1;
            if (currentFrame < maxFrame) {
                particle.setCurrentFrame(currentFrame);
            } else {
                particle.setCurrentFrame(modelAnimation.loop ? 0 : maxFrame - 1);
            }
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        TDParticle particle = (TDParticle) instance;
        doInit(particle);
        particle.renderSizeO = particle.renderSize;
        modelAnimation.setCurrentModel(particle);
        particle.setMaxFrame(modelAnimation.typeFrames.size());
    }

    private void doInit(TDParticle particle) {
        if (faceCameraMode.isEmpty() || faceCameraMode.get().isDirection()) {
            if (direction.mode() == Direction.Mode.CUSTOM_DIRECTION) {
                float[] values = direction.customDirection().calculate(particle);
                particle.setXRot(values[0]);
                particle.setYRot(values[1]);
                particle.setZRot(values[2]);
            } else if (direction.minSpeedThreshold() > 0.0F && Mth.lengthSquared(particle.getXd(), particle.getYd(), particle.getZd()) > particle.getPreset().minSpeedThresholdSqr) {
                particle.getFacingDirection().set(particle.getXd(), particle.getYd(), particle.getZd()).normalize();
            }
        }
        particle.renderSize = size.calculate(particle);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public record ModelAnimation(List<WeightedModel> typeFrames, float framesPerSecond, boolean stretchToLifetime, boolean loop) {
        public static final ModelAnimation EMPTY = new ModelAnimation(List.of(), 1, false, false);
        public static final Codec<ModelAnimation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.either(WeightedModel.CODEC.listOf(), WeightedModel.CODEC).xmap(
                        either -> either.map(Function.identity(), List::of),
                        list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
                ).fieldOf("type_frames").forGetter(ModelAnimation::typeFrames),
                ExtraCodecs.POSITIVE_FLOAT.lenientOptionalFieldOf("frames_per_second", 1.0F).forGetter(ModelAnimation::framesPerSecond),
                Codec.BOOL.lenientOptionalFieldOf("stretch_to_lifetime", false).forGetter(ModelAnimation::stretchToLifetime),
                Codec.BOOL.lenientOptionalFieldOf("loop", false).forGetter(ModelAnimation::loop)
        ).apply(instance, ModelAnimation::new));

        public void setCurrentModel(TDParticle particle) {
            ResourceLocation model = typeFrames.get(particle.getCurrentFrame()).get(particle.getLevel().random);
            ModelRenderer<?> renderer = RegisterTDPRendererEvent.getRenderer(model);
            particle.renderer = renderer;
            particle.typeIndex = getRenderState(renderer);
            particle.translucent = particle.typeIndex == 3;
        }

        private static int getRenderState(ModelRenderer<?> renderer) {
            if (renderer == ModelRenderer.DO_NOTHING) return -1;
            return renderer.getRenderType().index;
        }
    }

    public static class WeightedModel extends WeightedRandomList<WeightedModelEntry> {
        public static final Codec<WeightedModel> DIRECT_CODEC = WeightedModelEntry.CODEC.listOf().xmap(WeightedModel::new, WeightedRandomList::unwrap);
        public static final Codec<WeightedModel> CODEC = Codec.either(DIRECT_CODEC, ResourceLocation.CODEC).xmap(
                either -> either.map(Function.identity(), WeightedModel::new),
                model -> model.singleton ? Either.right(model.defaultModel) : Either.left(model)
        );

        private final boolean singleton;
        private final ResourceLocation defaultModel;

        public WeightedModel(List<WeightedModelEntry> entries) {
            super(entries);
            int size = entries.size();
            if (size == 0) {
                throw new IllegalArgumentException("Empty models is not allowed!");
            }
            this.singleton = size == 1;
            this.defaultModel = entries.getFirst().modelType;
        }

        public WeightedModel(ResourceLocation modelType) {
            this(List.of(new WeightedModelEntry(modelType)));
        }

        public ResourceLocation get(RandomSource random) {
            if (singleton) {
                return defaultModel;
            }
            return super.getRandom(random).map(WeightedModelEntry::modelType).orElse(defaultModel);
        }
    }

    public record WeightedModelEntry(ResourceLocation modelType, Weight weight) implements WeightedEntry {
        public static final Codec<WeightedModelEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("model_type").forGetter(WeightedModelEntry::modelType),
                Weight.CODEC.fieldOf("weight").forGetter(WeightedModelEntry::weight)
        ).apply(instance, WeightedModelEntry::new));

        public WeightedModelEntry(ResourceLocation modelType) {
            this(modelType, Weight.ONE);
        }

        @Override
        public Weight getWeight() {
            return weight;
        }
    }
}
