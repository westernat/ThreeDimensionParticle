package org.mesdag.thr_dim_particle.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.jetbrains.annotations.Nullable;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.impl.SimpleGeometryModelRenderer;
import org.mesdag.thr_dim_particle.client.impl.SimpleHardcodeModelRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

public class RegisterTDPRendererEvent extends Event implements IModBusEvent {
    private static Map<ModelType, ImmutableTriple<Function<EntityRendererProvider.Context, HardcodeModel.Renderer<?>>, @Nullable ModelLayerLocation, @Nullable Supplier<LayerDefinition>>> hardcodeCache;
    private static Map<ModelType, MutableTriple<@Nullable Function<EntityRendererProvider.Context, GeometryModel.Renderer<?>>, @Nullable ResourceLocation, @Nullable ModelResourceLocation>> geometryCache;
    private static Map<ModelType, ModelRenderer<?>> map;

    private RegisterTDPRendererEvent() {}

    public void registerHardcode(
            ResourceLocation modelId,
            Function<EntityRendererProvider.Context, HardcodeModel.Renderer<?>> provider,
            @Nullable ModelLayerLocation layerLocation,
            @Nullable Supplier<LayerDefinition> layerDefinition
    ) {
        if ((layerLocation == null) != (layerDefinition == null)) {
            throw new IllegalArgumentException("The layer definition must either be all null, or all non-null.");
        }
        hardcodeCache.put(new ModelType(ModelType.Variant.HARDCODE, modelId), new ImmutableTriple<>(provider, layerLocation, layerDefinition));
    }

    /// To invoke this method, you should register {@link LayerDefinition} by your self
    ///
    /// @see net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions
    public void registerHardcode(
            ResourceLocation modelId,
            Function<EntityRendererProvider.Context, HardcodeModel.Renderer<?>> provider
    ) {
        registerHardcode(modelId, provider, null, null);
    }

    public void registerHardcode(
            ResourceLocation modelId,
            ResourceLocation textureLocation,
            ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinition,
            Supplier<TDPRenderType> renderType
    ) {
        registerHardcode(modelId, context -> new SimpleHardcodeModelRenderer(context, layerLocation, textureLocation, renderType.get()), layerLocation, layerDefinition);
    }

    public void registerGeometry(ResourceLocation modelId, Function<EntityRendererProvider.Context, GeometryModel.Renderer<?>> provider) {
        geometryCache.put(new ModelType(ModelType.Variant.GEOMETRY, modelId), new MutableTriple<>(provider, null, null));
    }

    public void registerGeometry(ResourceLocation modelId) {
        geometryCache.put(new ModelType(ModelType.Variant.GEOMETRY, modelId), new MutableTriple<>(null, modelId, ModelResourceLocation.standalone(modelId)));
    }

    public static ModelRenderer<?> getRenderer(ResourceLocation type) {
        return map.getOrDefault(ModelType.Loader.INSTANCE.getModelTypes().get(type), ModelRenderer.DO_NOTHING);
    }

    public static void start() {
        hardcodeCache = new HashMap<>();
        geometryCache = new HashMap<>();
    }

    public static void postEvent() {
        ModLoader.postEvent(new RegisterTDPRendererEvent());
        ImmutableMap.Builder<ResourceLocation, ModelType> builder = ImmutableMap.builder();
        for (ModelType type : hardcodeCache.keySet()) {
            builder.put(type.modelId(), type);
        }
        for (ModelType type : geometryCache.keySet()) {
            builder.put(type.modelId(), type);
        }
        ModelType.Loader.INSTANCE.builtinModelTypes = builder.build();
    }

    public static void registerLayerDefinitions(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> registration) {
        for (var triple : hardcodeCache.values()) {
            if (triple.middle == null || triple.right == null) continue;
            registration.accept(triple.middle, triple.right);
        }
    }

    // 自己写的models/<modelId>.json需要手动注册模型
    public static void registerGeometries(Consumer<ModelResourceLocation> registration) {
        for (Map.Entry<ResourceLocation, ModelType> entry : ModelType.Loader.INSTANCE.getModelTypes().entrySet()) {
            ModelType type = entry.getValue();
            if (type.variant() != ModelType.Variant.GEOMETRY) continue;
            ResourceLocation modelId = type.modelId();
            geometryCache.put(type, new MutableTriple<>(null, modelId, ModelResourceLocation.standalone(modelId)));
        }
        for (var triple : geometryCache.values()) {
            if (triple.right == null) continue;
            registration.accept(triple.right);
        }
    }

    public static void cacheGeometriesRenderType(Predicate<ModelResourceLocation> modelExistsChecker, Function<ResourceLocation, BlockModel> blockModelGetter) {
        for (var triple : geometryCache.values()) {
            if (triple.middle == null || triple.right == null || !modelExistsChecker.test(triple.right)) continue;
            BlockModel blockModel = blockModelGetter.apply(ModelBakery.MODEL_LISTER.idToFile(triple.middle));
            if (blockModel == null) continue;
            ResourceLocation hint = blockModel.customData.getRenderTypeHint();
            if (hint == null || !TDP.MODID.equals(hint.getNamespace())) continue;
            TDPRenderType renderType = TDPRenderType.get(hint.getPath());
            triple.left = context -> new SimpleGeometryModelRenderer(context, triple.right, renderType);
        }
    }

    public static void end() {
        ImmutableMap.Builder<ModelType, ModelRenderer<?>> builder = ImmutableMap.builder();
        Minecraft minecraft = Minecraft.getInstance();
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(
                minecraft.getEntityRenderDispatcher(),
                minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(),
                minecraft.gameRenderer.itemInHandRenderer,
                minecraft.getResourceManager(),
                minecraft.getEntityModels(),
                minecraft.font
        );
        for (var entry : hardcodeCache.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().left.apply(context));
        }
        hardcodeCache = null;
        for (var entry : geometryCache.entrySet()) {
            var provider = entry.getValue().left;
            if (provider == null) continue;
            builder.put(entry.getKey(), provider.apply(context));
        }
        geometryCache = null;
        map = builder.build();
    }
}
