package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.FaceCameraMode;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.thr_dim_particle.TDP;
import org.mesdag.thr_dim_particle.client.compat.sodium.IrisHelper;
import org.mesdag.thr_dim_particle.client.impl.TDParticleAppearance;
import org.mesdag.thr_dim_particle.client.impl.WithBlockParticleEmitter;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

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
    public static ParticleBuffer[] buffers;

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

    @SubscribeEvent
    public static void modConfig$Loading(ModConfigEvent.Loading event) {
        if (TDP.MODID.equals(event.getConfig().getModId())) {
            ClientConfigs.onLoad();
        }
    }

    @SubscribeEvent
    public static void modConfig$Reloading(ModConfigEvent.Reloading event) {
        if (TDP.MODID.equals(event.getConfig().getModId())) {
            ClientConfigs.onLoad();
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
//        event.registerHardcode(
//                TDP.asResource("test_model2"),
//                TDP.asResource("tdp/test_hardcode"),
//                TestHardcodeModel.LAYER_LOCATION,
//                TestHardcodeModel::createBodyLayer,
//                () -> TDPRenderType.get(0)
//        );
    }

    @SubscribeEvent
    public static void attachEmitterToBlock(AttachEmitterToBlockEvent event) {
        simpleAttach(event, Blocks.END_ROD, EndRodBlock.FACING, facing -> "v.x=" + facing.getStepX() + ";v.y=" + facing.getStepY() + ";v.z=" + facing.getStepZ(), ClientConfigs.endRod);
        simpleAttach(event, Blocks.NETHER_PORTAL, NetherPortalBlock.AXIS, axis -> "v.x=" + (axis == Direction.Axis.X ? 1 : 0), ClientConfigs.netherPortal);
    }

    private static <T extends Comparable<T>> void simpleAttach(AttachEmitterToBlockEvent event, Block block, Property<T> property, Function<T, String> expStr, ClientConfigs.ParticleConfig config) {
        BlockState blockState = block.defaultBlockState();
        ResourceLocation particle = asParticle(BuiltInRegistries.BLOCK.getKey(block).getPath());
        List<AttachEmitterToBlockEvent.AttachData> associated = new ArrayList<>();
        for (T t : property.getPossibleValues()) {
            associated.add(event.attach(blockState.setValue(property, t), particle, (level, pos, state) -> new MolangExp(expStr.apply(t)), false));
        }
        config.initAssociated(associated);
    }

    @SubscribeEvent
    public static void clientTick$Post(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null && level.getGameTime() % ClientConfigs.emitterAutoRemoveIntervalTick == 0) {
            Camera camera = minecraft.gameRenderer.getMainCamera();
            if (camera.isInitialized()) {
                AttachEmitterToBlockEvent.tick(camera);
                tick(camera);
            }
        }
    }

    @SubscribeEvent
    public static void clientPlayerNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AttachEmitterToBlockEvent.clearEmitters();
    }

    public static void render(Queue<Particle> queue, Camera camera, float partialTick, Frustum frustum, boolean isSolid) {
        GlStateManager.BlendState blend = GlStateManager.BLEND;
        blend.mode.enable();
        if (770 != blend.srcRgb || 771 != blend.dstRgb || 1 != blend.srcAlpha || 0 != blend.dstAlpha) {
            blend.srcRgb = 770;
            blend.dstRgb = 771;
            blend.srcAlpha = 1;
            blend.dstAlpha = 0;
            GL14.glBlendFuncSeparate(770, 771, 1, 0);
        }
        if (!GlStateManager.DEPTH.mask) {
            GlStateManager.DEPTH.mask = true;
            GL11.glDepthMask(true);
        }
        for (Particle particle : queue) {
            TDParticle tdp = (TDParticle) particle;
            ParticleBuffer buffer = tdp.buffer;
            if (buffer == null) {
                continue;
            } else if (isSolid) {
                if (tdp.translucent) continue;
            } else if (!tdp.translucent) {
                continue;
            } else {
                AABB aabb = tdp.renderBoundingBox;
                if (!frustum.cubeInFrustum(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)) {
                    continue;
                }
            }
            try {
                tdp.render(buffer, camera, partialTick);
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                CrashReportCategory category = report.addCategory("Particle being rendered");
                category.setDetail("Particle", tdp::toString);
                category.setDetail("Particle Type", TDPClient.TDP_RENDER_TYPE::toString);
                throw new ReportedException(report);
            }
        }
        for (int i = 0; i < 4; i++) {
            if (buffers[i].storeMesh()) {
                TDPRenderType.get(i).draw();
            }
        }
    }

    public static TextureAtlas getAtlas() {
        if (atlas == null) {
            atlas = Minecraft.getInstance().getModelManager().getAtlas(ATLAS_LOCATION);
        }
        return atlas;
    }

    static final ArrayDeque<ParticleEmitter> emitters = new ArrayDeque<>(64);

    private static void tick(Camera camera) {
        if (emitters.isEmpty()) return;
        Iterator<ParticleEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter emitter = iterator.next();
            if (shouldRemoveEmitter(camera, emitter)) {
                emitter.remove();
                iterator.remove();
            }
        }
    }

    public static boolean addEmitter(Level level, Vec3 pos, ResourceLocation particle, Variable... variables) {
        if (ableToAddEmitter()) {
            ParticleEmitter emitter = new ParticleEmitter(level, pos, particle) {
                @Override
                protected void createVars() {
                    super.createVars();
                    for (Variable var : variables) {
                        vars.table.put(var.name(), var);
                    }
                }
            };
            PSGameClient.LOADER.addEmitter(emitter, false);
            emitters.add(emitter);
            return false;
        }
        return ClientConfigs.allowsVanillaParticleWhenReachLimit;
    }

    public static boolean ableToAddEmitter() {
        return Minecraft.fps > ClientConfigs.fpsThreshold &&
                AttachEmitterToBlockEvent.emitters.size() + emitters.size() < ClientConfigs.emitterLimit;
    }

    public static boolean shouldRemoveEmitter(Camera camera, ParticleEmitter emitter) {
        if (emitter.isRemoved()) return true;
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
}
