package org.mesdag.thr_dim_particle.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.common.IExtensibleEnum;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public record ModelType(Variant variant, ResourceLocation modelId) {
    public static final Codec<ModelType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Variant.CODEC.fieldOf("variant").forGetter(ModelType::variant),
            ResourceLocation.CODEC.fieldOf("model_id").forGetter(ModelType::modelId)
    ).apply(instance, ModelType::new));

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof ModelType t && variant == t.variant && modelId.equals(t.modelId));
    }

    @Override
    public int hashCode() {
        int result = variant.hashCode();
        result = 31 * result + modelId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ModelType{" +
                "variant=" + variant +
                ", modelId=" + modelId +
                '}';
    }

    public enum Variant implements StringRepresentable, IExtensibleEnum {
        HARDCODE("hardcode"),
        GEOMETRY("geometry"),
        GECKOLIB("geckolib");

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Variant create(String name, String variantName) {
            throw new IllegalStateException("Enum not extended");
        }
    }

    public static final class Loader {
        public static final Loader INSTANCE = new Loader();
        private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("particle_models");

        Map<ResourceLocation, ModelType> builtinModelTypes = ImmutableMap.of();
        private Map<ResourceLocation, ModelType> modelTypes = ImmutableMap.of();

        private Loader() {}

        public void load(ResourceManager resourceManager) {
            ImmutableMap.Builder<ResourceLocation, ModelType> builder = ImmutableMap.builder();
            for (Map.Entry<ResourceLocation, Resource> entry : MODEL_LISTER.listMatchingResources(resourceManager).entrySet()) {
                ResourceLocation id = MODEL_LISTER.fileToId(entry.getKey());
                try (Reader reader = entry.getValue().openAsReader()) {
                    builder.put(id, ModelType.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader)).getOrThrow(false, msg -> {
                        throw new JsonParseException(msg);
                    }));
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to load model for particle " + id, exception);
                }
            }
            this.modelTypes = builder.putAll(builtinModelTypes).build();
        }

        public Map<ResourceLocation, ModelType> getModelTypes() {
            return modelTypes;
        }
    }
}
