package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMaterialAtlasesEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.api.RegisterCustomComponentEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.particle.FaceCameraMode;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.compat.sodium.IrisHelper;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance;

import java.io.IOException;
import java.util.Iterator;
import java.util.Queue;

@Mod(value = TDP.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TDP.MODID, value = Dist.CLIENT)
public class TDPClient {
    public static final boolean IRIS_LOADED = LoadingModList.get().getModFileById("iris") != null;
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
    public static final ResourceLocation ATLAS_LOCATION = TDP.asResource("textures/atlas/particles.png");
    public static final int BUFFER_SIZE = 262144;
    static ShaderInstance particleSolidShaderInstance;
    static ShaderInstance particleCutoutShaderInstance;
    static ShaderInstance particleCutoutMippedShaderInstance;
    static ShaderInstance particleTranslucentShaderInstance;
    private static TextureAtlas atlas;
    public static ParticleBuffer[] buffers;

    public TDPClient() {
        RegisterTDPRendererEvent.start();
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ParticleStorm.IRIS_LOADED) {
                IrisHelper.setAllowUnknownShaders();
            }
            buffers = new ParticleBuffer[]{
                    new ParticleBuffer(new ByteBufferBuilder(BUFFER_SIZE)),
                    new ParticleBuffer(new ByteBufferBuilder(BUFFER_SIZE)),
                    new ParticleBuffer(new ByteBufferBuilder(BUFFER_SIZE)),
                    new ParticleBuffer(new ByteBufferBuilder(BUFFER_SIZE))
            };
        });
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
    public static void registerCustomComponent(RegisterCustomComponentEvent event) {
        event.register(TDParticleAppearance.ID, TDParticleAppearance.CODEC);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_solid"), TDPRenderType.FORMAT), instance -> particleSolidShaderInstance = instance);
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_cutout"), TDPRenderType.FORMAT), instance -> particleCutoutShaderInstance = instance);
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_cutout_mipped"), TDPRenderType.FORMAT), instance -> particleCutoutMippedShaderInstance = instance);
        event.registerShader(new ShaderInstance(provider, TDP.asResource("particle_translucent"), TDPRenderType.FORMAT), instance -> particleTranslucentShaderInstance = instance);
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

    @SubscribeEvent
    public static void registerMaterialAtlasesEvent(RegisterMaterialAtlasesEvent event) {
        event.register(ATLAS_LOCATION, TDP.asResource("particles"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void render(Queue<Particle> queue, Camera camera, float partialTick, Frustum frustum, boolean isOpaque) {
        Iterator<TDParticle> iterator = (Iterator) queue.iterator();
        while (iterator.hasNext()) {
            TDParticle tdp = iterator.next();
            if (isOpaque == tdp.translucent || tdp.buffer == null || !tdp.isVisible(camera, frustum, partialTick)) {
                continue;
            }
            try {
                tdp.render(tdp.buffer, camera, partialTick);
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.forThrowable(throwable, "Rendering 3D Particle");
                CrashReportCategory category = report.addCategory("3D Particle being rendered");
                category.setDetail("3D Particle id", tdp.preset.effect.description.identifier()::toString);
                throw new ReportedException(report);
            }
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        if (TDPClient.IRIS_LOADED && IrisHelper.hasShader()) {
            for (int i = 0; i < 4; i++) {
                MeshData meshData = buffers[i].storeMesh();
                if (meshData == null) continue;
                TDPRenderType.get(i).draw(meshData);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                buffers[i].draw(TDPRenderType.get(i));
            }
        }
    }

    public static TextureAtlas getAtlas() {
        if (atlas == null) {
            atlas = Minecraft.getInstance().getModelManager().getAtlas(ATLAS_LOCATION);
        }
        return atlas;
    }

    public static short light2Short(int packetLight) {
        return (short) ((LightTexture.sky(packetLight) << 4) | LightTexture.block(packetLight));
    }
}
