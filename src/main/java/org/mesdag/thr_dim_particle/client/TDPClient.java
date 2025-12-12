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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.FaceCameraMode;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.compat.sodium.IrisHelper;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance;
import org.mesdag.thr_dim_particle.client.impl.TestHardcodeModel;
import org.mesdag.thr_dim_particle.client.impl.WithBlockParticleEmitter;

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
    public static final ResourceLocation ATLAS_LOCATION = TDP.asResource("textures/atlas/particles.png");
    static ShaderInstance particleSolidShaderInstance;
    static ShaderInstance particleCutoutShaderInstance;
    static ShaderInstance particleCutoutMippedShaderInstance;
    static ShaderInstance particleTranslucentShaderInstance;
    private static TextureAtlas atlas;
    private static ParticleBuffer[] buffers;

    public static final ResourceLocation EXPLOSION_PARTICLE = ResourceLocation.fromNamespaceAndPath("tdp", "bomb_smoke");

    public TDPClient(ModContainer container) {
        ClientConfigs.register(container);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        RegisterTDPRendererEvent.start();
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("iris")) {
                IrisHelper.setAllowUnknownShaders();
            }
            AttachEmitterToBlockEvent.postEvent();
            ByteBufferBuilder buffer = Tesselator.getInstance().buffer;
            buffers = new ParticleBuffer[]{
                    new ParticleBuffer(buffer),
                    new ParticleBuffer(buffer),
                    new ParticleBuffer(buffer),
                    new ParticleBuffer(buffer)
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
    public static void registerCustomEmitterType(RegisterCustomEmitterTypeEvent event) {
        event.register(TDP.asResource("type"), WithBlockParticleEmitter::new);
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

    @SubscribeEvent
    public static void registerTDPRenderer(RegisterTDPRendererEvent event) {
        event.registerHardcode(
                TDP.asResource("test_model2"),
                TDP.asResource("tdp/test_hardcode"),
                TestHardcodeModel.LAYER_LOCATION,
                TestHardcodeModel::createBodyLayer,
                () -> TDPRenderType.get(0)
        );
    }

    @SubscribeEvent
    public static void attachEmitterToBlock(AttachEmitterToBlockEvent event) {
        event.attach(Blocks.END_ROD, ResourceLocation.fromNamespaceAndPath("snowstorm", "loading"), MolangExp.EMPTY, false);
    }

    @SubscribeEvent
    public static void clientTick$Post(ClientTickEvent.Post event) {
        AttachEmitterToBlockEvent.tick();
    }

    @SubscribeEvent
    public static void clientPlayerNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AttachEmitterToBlockEvent.clearEmitters();
    }

    public static void render(Queue<Particle> queue, Camera camera, float partialTick, Frustum frustum) {
        ParticleBuffer buffer;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        for (Particle particle : queue) {
            TDParticle tdp = (TDParticle) particle;
            if (tdp.renderer == ModelRenderer.DO_NOTHING) continue;
            if (!frustum.isVisible(tdp.renderBoundingBox)) continue;
            try {
                tdp.renderFast(buffers[tdp.renderer.getRenderType().index], camera, partialTick);
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                CrashReportCategory category = report.addCategory("Particle being rendered");
                category.setDetail("Particle", tdp::toString);
                category.setDetail("Particle Type", TDPClient.TDP_RENDER_TYPE::toString);
                throw new ReportedException(report);
            }
        }
        for (int i = 0; i < 4; i++) {
            buffer = buffers[i];
            MeshData data = buffer.storeMesh();
            buffer.vertices = 0;
            if (data == null) continue;
            TDPRenderType.get(i).draw(data);
        }
    }

    public static TextureAtlas getAtlas() {
        if (atlas == null) {
            atlas = Minecraft.getInstance().getModelManager().getAtlas(ATLAS_LOCATION);
        }
        return atlas;
    }
}
