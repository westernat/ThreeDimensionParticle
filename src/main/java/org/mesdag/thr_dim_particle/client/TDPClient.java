package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.particle.FaceCameraMode;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance;

import java.io.IOException;
import java.util.Queue;

@Mod(value = TDP.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TDP.MODID, value = Dist.CLIENT)
public class TDPClient {
    public static final ParticleRenderType TDP_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public @Nullable BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            return null;
        }

        @Override
        public String toString() {
            return "TDP";
        }
    };
    static ShaderInstance particleSolidShaderInstance;
    static ShaderInstance particleCutoutShaderInstance;
    static ShaderInstance particleCutoutMippedShaderInstance;
    static ShaderInstance particleTranslucentShaderInstance;

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
    public static void particlePresetLoaded(ParticlePresetLoadedEvent event) {
        ParticlePreset preset = event.getPreset();
        if (preset.effect.description.type() == TDP.TDP.get() &&
                preset.effect.components.get(TDParticleAppearance.ID) instanceof TDParticleAppearance component &&
                component.faceCameraMode().isPresent()
        ) {
            preset.facingCameraMode = FaceCameraMode.fromComponent(component.faceCameraMode().get());
        }
    }

    public static void render(Queue<Particle> instance, Camera camera, float partialTick, Frustum frustum, ParticleRenderType particleRenderType) {
        BufferBuilder[] builders = new BufferBuilder[4];
        for (Particle particle : instance) {
            if (!frustum.isVisible(particle.getRenderBoundingBox(partialTick))) continue;
            TDParticle tdp = (TDParticle) particle;
            if (tdp.renderer == ModelRenderer.DO_NOTHING) continue;
            try {
                TDPRenderType renderType = tdp.renderer.getRenderType(tdp);
                BufferBuilder builder = builders[renderType.index];
                if (builder == null) {
                    builders[renderType.index] = builder = Tesselator.getInstance().begin(renderType.mode, renderType.format);
                }
                tdp.renderFast(builder, camera, partialTick);
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                CrashReportCategory category = report.addCategory("Particle being rendered");
                category.setDetail("Particle", particle::toString);
                category.setDetail("Particle Type", particleRenderType::toString);
                throw new ReportedException(report);
            }
        }
        for (int i = 0; i < 4; i++) {
            BufferBuilder builder = builders[i];
            if (builder == null) continue;
            MeshData data = builder.build();
            if (data == null) continue;
            TDPRenderType.get(i).draw(data);
        }
    }
}
