package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.particle.FaceCameraMode;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance;

import java.io.IOException;

@Mod(value = TDP.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TDP.MODID, value = Dist.CLIENT)
public class TDPClient {
    public static final ResourceLocation ATLAS = TextureAtlas.LOCATION_BLOCKS; // todo 换particle图集
    private static ShaderInstance particleSolidShaderInstance;
    private static RenderType particleSolidRenderType;

    public static RenderType getParticleSolidRenderType() {
        if (particleSolidRenderType == null) {
            particleSolidRenderType = RenderType.create(
                    "tdp_particle_solid",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleSolidShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            );
        }
        return particleSolidRenderType;
    }

    private static ShaderInstance particleCutoutShaderInstance;
    private static RenderType particleCutoutRenderType;

    public static RenderType getParticleCutoutRenderType() {
        if (particleCutoutRenderType == null) {
            particleCutoutRenderType = RenderType.create(
                    "tdp_particle_cutout",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            );
        }
        return particleCutoutRenderType;
    }

    private static ShaderInstance particleCutoutMippedShaderInstance;
    private static RenderType particleCutoutMippedRenderType;

    public static RenderType getParticleCutoutMippedRenderType() {
        if (particleCutoutMippedRenderType == null) {
            particleCutoutMippedRenderType = RenderType.create(
                    "tdp_particle_cutout_mipped",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutMippedShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, true))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            );
        }
        return particleCutoutMippedRenderType;
    }

    private static ShaderInstance particleTranslucentShaderInstance;
    private static RenderType particleTranslucentRenderType;

    public static RenderType getParticleTranslucentRenderType() {
        if (particleTranslucentRenderType == null) {
            particleTranslucentRenderType = RenderType.create(
                    "tdp_particle_translucent",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleTranslucentShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, false))
                            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            );
        }
        return particleTranslucentRenderType;
    }

    public TDPClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        RegisterTDPRendererEvent.start();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.PARTICLE_TYPE) {
            RegisterTDPRendererEvent.postEvent();
        }
    }

    @SubscribeEvent
    public static void registerCustomParticleType(RegisterCustomParticleTypeEvent event) {
        event.register(TDP.TDP.get(), (emitter, particlePreset, level, x, y, z) ->
                new TDParticle(particlePreset, level, x, y, z)
        );
    }

    @SubscribeEvent
    public static void entityRenderers$RegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        RegisterTDPRendererEvent.registerLayerDefinitions(event::registerLayerDefinition);
    }

    @SubscribeEvent
    public static void model$RegisterAdditional(ModelEvent.RegisterAdditional event) {
        ModelType.Loader.INSTANCE.load(Minecraft.getInstance().getResourceManager());
        RegisterTDPRendererEvent.registerGeometries(event::register);
    }

    @SubscribeEvent
    public static void model$BakingCompleted(ModelEvent.BakingCompleted event) {
        RegisterTDPRendererEvent.cacheGeometriesRenderType(event.getModels()::containsKey, event.getModelBakery().modelResources::get);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        IComponent.register(TDParticleAppearance.ID, TDParticleAppearance.CODEC);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_solid"), DefaultVertexFormat.NEW_ENTITY), instance -> particleSolidShaderInstance = instance);
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_cutout"), DefaultVertexFormat.NEW_ENTITY), instance -> particleCutoutShaderInstance = instance);
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_cutout_mipped"), DefaultVertexFormat.NEW_ENTITY), instance -> particleCutoutMippedShaderInstance = instance);
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_translucent"), DefaultVertexFormat.NEW_ENTITY), instance -> particleTranslucentShaderInstance = instance);
    }

    @SubscribeEvent
    public static void registerNamedRenderTypes(RegisterNamedRenderTypesEvent event) {
        event.register(TDP.asResource("solid"), RenderType.solid(), getParticleSolidRenderType(), NeoForgeRenderTypes.ITEM_LAYERED_SOLID.get());
        event.register(TDP.asResource("cutout"), RenderType.cutout(), getParticleCutoutRenderType(), NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT.get());
        event.register(TDP.asResource("cutout_mipped"), RenderType.cutoutMipped(), getParticleCutoutMippedRenderType(), NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT_MIPPED.get());
        event.register(TDP.asResource("translucent"), RenderType.translucent(), getParticleTranslucentRenderType(), NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get());
    }

    @SubscribeEvent
    public static void particlePresetLoaded(ParticlePresetLoadedEvent event) {
        ParticlePreset preset = event.getPreset();
        if (preset.effect.description.type() == TDP.TDP.get() &&
                preset.effect.components.get(TDParticleAppearance.ID) instanceof TDParticleAppearance component &&
                component.faceCameraMode().isPresent()
        ) {
            preset.facingCameraMode = FaceCameraMode.fromComponent(component.faceCameraMode().get());
        }
    }
}
