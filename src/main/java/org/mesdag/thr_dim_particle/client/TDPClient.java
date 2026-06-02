package org.mesdag.thr_dim_particle.client;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.api.RegisterCustomComponentEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.FaceCameraMode;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance;
import org.mesdag.thr_dim_particle.client.impl.emitter.PresetVarsParticleEmitter;
import org.mesdag.thr_dim_particle.client.impl.emitter.TDParticleEmitter;
import org.mesdag.thr_dim_particle.client.impl.emitter.WithBlockParticleEmitter;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = TDP.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TDPClient {
    public static final ParticleRenderType TDP_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {}

        @Override
        public void end(Tesselator tesselator) {}

        @Override
        public String toString() {
            return "TDP";
        }
    };
    public static final ResourceLocation ATLAS_LOCATION = TDP.asResource("textures/atlas/particles.png");
    public static final int BUFFER_SIZE = 262144;
    public static final String RESOURCE_PACK_PATH = "3d_particle_display_adaptation";
    static ShaderInstance particleSolidShaderInstance;
    static ShaderInstance particleCutoutShaderInstance;
    static ShaderInstance particleCutoutMippedShaderInstance;
    static ShaderInstance particleTranslucentShaderInstance;
    private static TextureAtlas atlas;
    public static ParticleBuffer[] buffers;

    public static void init(ModLoadingContext context) {
        ClientConfigs.register(context);
        RegisterTDPRendererEvent.start();
        MinecraftForge.EVENT_BUS.addListener(TDPClient::particlePresetLoaded);
        MinecraftForge.EVENT_BUS.addListener(TDPClient::clientTick$Post);
        MinecraftForge.EVENT_BUS.addListener(TDPClient::clientPlayerNetwork$LoggingOut);
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AttachEmitterToBlockEvent.postEvent();
            buffers = new ParticleBuffer[]{
                    new ParticleBuffer(new BufferBuilder(BUFFER_SIZE)),
                    new ParticleBuffer(new BufferBuilder(BUFFER_SIZE)),
                    new ParticleBuffer(new BufferBuilder(BUFFER_SIZE)),
                    new ParticleBuffer(new BufferBuilder(BUFFER_SIZE))
            };
        });
    }

    @SubscribeEvent
    public static void modConfig$Loading(ModConfigEvent.Loading event) {
        if (TDP.MODID.equals(event.getConfig().getModId())) {
            ClientConfigs.onLoad(false);
        }
    }

    @SubscribeEvent
    public static void modConfig$Reloading(ModConfigEvent.Reloading event) {
        if (TDP.MODID.equals(event.getConfig().getModId())) {
            ClientConfigs.onLoad(true);
        }
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

    private static void particlePresetLoaded(ParticlePresetLoadedEvent event) {
        ParticlePreset preset = event.getPreset();
        if (preset.effect.description.type() == TDP.TDP.get() &&
                preset.effect.components.get(TDParticleAppearance.ID) instanceof TDParticleAppearance component &&
                component.faceCameraMode().isPresent()
        ) {
            preset.facingCameraMode = FaceCameraMode.fromComponent(component.faceCameraMode().get());
        }
    }

    private static void clientTick$Post(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null) {
            int tick = ClientConfigs.emitterAutoRemoveIntervalTick;
            if (tick <= 1 || level.getGameTime() % tick == 0) {
                Camera camera = minecraft.gameRenderer.getMainCamera();
                if (camera.isInitialized()) {
                    AttachEmitterToBlockEvent.tick(camera);
                    tick(camera);
                }
            }
        }
    }

    private static void clientPlayerNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AttachEmitterToBlockEvent.clearEmitters();
    }

    @SuppressWarnings("WhileLoopReplaceableByForEach")
    public static void render(Queue<Particle> queue, Camera camera, float partialTick, Frustum frustum, boolean isOpaque) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        for (ParticleBuffer buffer : buffers) {
            buffer.begin();
        }
        Iterator<Particle> iterator = queue.iterator();
        while (iterator.hasNext()) {
            TDParticle tdp = (TDParticle) iterator.next();
            if (isOpaque == tdp.translucent) {
                continue;
            }
            ParticleBuffer buffer = tdp.buffer;
            if (buffer == null) {
                continue;
            }
            if (!tdp.isVisible(camera, frustum, partialTick)) {
                continue;
            }
            try {
                tdp.render(buffer, camera, partialTick);
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.forThrowable(throwable, "Rendering 3D Particle");
                CrashReportCategory category = report.addCategory("3D Particle being rendered");
                category.setDetail("3D Particle", tdp.emitter.particleId::toString);
                throw new ReportedException(report);
            }
        }
        for (int i = 0; i < 4; i++) {
            BufferBuilder buffer = buffers[i].end();
            if (buffer != null) {
                TDPRenderType.get(i).end(buffer, VertexSorting.DISTANCE_TO_ORIGIN);
            }
        }
    }

    public static TextureAtlas getAtlas() {
        if (atlas == null) {
            atlas = Minecraft.getInstance().getModelManager().getAtlas(ATLAS_LOCATION);
        }
        return atlas;
    }

    public static final Queue<TDParticleEmitter> emitters = new ArrayDeque<>(64);
    public static final Map<BlockPos, TDParticleEmitter> campfireEmitters = new Object2ObjectOpenHashMap<>(64);
    private static final Iterable<TDParticleEmitter> emittersIterable = Iterables.concat(emitters, campfireEmitters.values());

    private static void tick(Camera camera) {
        if (emitters.isEmpty() && campfireEmitters.isEmpty()) return;
        Iterator<TDParticleEmitter> iterator = emittersIterable.iterator();
        while (iterator.hasNext()) {
            TDParticleEmitter emitter = iterator.next();
            List<ParticleEmitter> children = emitter.getChildren(false);
            if (children != null) {
                for (ParticleEmitter child : children) {
                    if (child instanceof TDParticleEmitter tdpe && shouldRemoveEmitter(camera, tdpe)) {
                        child.remove();
                    }
                }
            }
            if (shouldRemoveEmitter(camera, emitter)) {
                emitter.remove();
                iterator.remove();
            }
        }
    }

    public static boolean addEmitter(Level level, Vec3 pos, ResourceLocation particle, Variable... variables) {
        if (ableToAddEmitter()) {
            PresetVarsParticleEmitter emitter = new PresetVarsParticleEmitter(level, pos, particle, false, variables);
            MolangParticleEngine.INSTANCE.addEmitter(emitter);
            emitters.add(emitter);
            return false;
        }
        return ClientConfigs.allowsVanillaParticleWhenReachLimit;
    }

    public static boolean ableToAddEmitter() {
        return Minecraft.fps > ClientConfigs.fpsThreshold &&
                AttachEmitterToBlockEvent.emitters.size() + emitters.size() < ClientConfigs.emitterLimit;
    }

    public static boolean shouldRemoveEmitter(Camera camera, TDParticleEmitter emitter) {
        if (emitter.isRemoved()) return true;
        if (emitter.ignoreRange) return false;
        return isFarAwayFromCamera(camera, emitter);
    }

    public static boolean isFarAwayFromCamera(Camera camera, TDParticleEmitter emitter) {
        double v = camera.getPosition().distanceToSqr(emitter.getPosition());
        if (v < Mth.square(ClientConfigs.emitterAutoRemoveMinimumDistance)) return false;
        v = Math.sqrt(v) - ClientConfigs.emitterAutoRemoveMinimumDistance;
        double c = 0;
        do {
            c += ClientConfigs.emitterAutoRemoveAttenuationCoefficient;
            if (emitter.level.random.nextDouble() < c) {
                return true;
            }
            v -= ClientConfigs.emitterAutoRemoveAttenuationDistance;
        } while (v > 0 && c < 1);
        return false;
    }

    public static ResourceLocation asParticle(String path) {
        return ResourceLocation.fromNamespaceAndPath("tdp", path);
    }

    public static short light2Short(int packetLight) {
        return (short) ((LightTexture.sky(packetLight) << 4) | LightTexture.block(packetLight));
    }

    public static boolean isResourcePackLoaded() {
        return Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(TDP.MODID + ":" + TDPClient.RESOURCE_PACK_PATH);
    }
}
