package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.component.ParticleMotionCollision;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.particle.*;

import java.util.List;
import java.util.Optional;

import static org.mesdag.particlestorm.particle.MolangParticleInstance.FULL_LIGHT;

public class TDParticle extends Particle implements IMolangParticleInstance {
    protected final ParticlePreset preset;
    protected ParticleVariableTable vars;

    protected Vector3f acceleration = new Vector3f();
    protected Vector3f facingDirection = new Vector3f();
    protected Vector3f initialSpeed = new Vector3f();
    protected float xRot = 0.0F;
    protected float yRot = 0.0F;
    protected float xRotO = 0.0F;
    protected float yRotO = 0.0F;
    protected float zRotD = 0.0F;
    protected boolean hasCollision = false;
    protected float collisionDrag = 0.0F;
    protected float coefficientOfRestitution = 0.0F;
    protected boolean expireOnContact = false;

    protected final double particleRandom1;
    protected final double particleRandom2;
    protected final double particleRandom3;
    protected final double particleRandom4;
    protected List<IParticleComponent> components;
    protected ParticleEmitter emitter;

    protected boolean insideKillPlane;
    protected ParticleGroup particleGroup;
    protected int lastTimeline = 0;

    public ModelRenderer<?> renderer = ModelRenderer.DO_NOTHING;
    public @Nullable ParticleBuffer buffer;
    public boolean translucent;
    public int abgr = 0xFFFFFFFF;
    public short light;
    public float[] renderSize = new float[3];
    public float[] renderSizeO = new float[3];
    public AABB renderBoundingBox;
    protected int maxFrame = 1;
    protected int currentFrame = 0;

    public TDParticle(ParticlePreset particlePreset, ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.friction = 1.0F;
        this.preset = particlePreset;
        this.light = getLightColor();

        RandomSource random = level.getRandom();
        this.particleRandom1 = random.nextDouble();
        this.particleRandom2 = random.nextDouble();
        this.particleRandom3 = random.nextDouble();
        this.particleRandom4 = random.nextDouble();
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void setEmitter(ParticleEmitter emitter) {
        this.emitter = emitter;
        this.vars = new ParticleVariableTable(preset.vars, emitter.getVars());
    }

    @Override
    public ParticlePreset getPreset() {
        return preset;
    }

    @Override
    public @Nullable TextureAtlasSprite getSprite() {
        return null;
    }

    @Override
    public Vector3f getAcceleration() {
        return acceleration;
    }

    @Override
    public Vector3f getFacingDirection() {
        return facingDirection;
    }

    @Override
    public Vector3f getInitialSpeed() {
        return initialSpeed;
    }

    @Override
    public void setXRot(float x) {
        this.xRot = x;
    }

    @Override
    public void setYRot(float y) {
        this.yRot = y;
    }

    @Override
    public void setZRot(float z) {
        this.roll = z;
    }

    @Override
    public void setZRotD(float delta) {
        this.zRotD = delta;
    }

    @Override
    public float getZRotD() {
        return zRotD;
    }

    @Override
    public void setCollisionDrag(float drag) {
        this.collisionDrag = drag;
    }

    @Override
    public void setCoefficientOfRestitution(float coefficient) {
        this.coefficientOfRestitution = coefficient;
    }

    @Override
    public void setExpireOnContact(boolean b) {
        this.expireOnContact = b;
    }

    @Override
    public void setComponents(List<IParticleComponent> components) {
        this.components = components;
    }

    @Override
    public float getScaleU() {
        return 0;
    }

    @Override
    public float getScaleV() {
        return 0;
    }

    @Override
    public void setBillboardSize(float[] size) {}

    @Override
    public void setUvSize(float[] size) {}

    @Override
    public float[] getUvSize() {
        return new float[2];
    }

    @Override
    public void setUvStep(float[] step) {}

    @Override
    public float[] getUvStep() {
        return new float[2];
    }

    @Override
    public void setUV(float u, float v, float w, float h) {}

    @Override
    public void setMaxFrame(int frame) {
        this.maxFrame = frame;
    }

    @Override
    public int getMaxFrame() {
        return maxFrame;
    }

    @Override
    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }

    @Override
    public int getCurrentFrame() {
        return currentFrame;
    }

    @Override
    public void setInsideKillPlane(boolean b) {
        this.insideKillPlane = b;
    }

    @Override
    public boolean isInsideKillPlane() {
        return insideKillPlane;
    }

    @Override
    public void setParticleGroup(ParticleGroup group) {
        this.particleGroup = group;
    }

    @Override
    public void setLastTimeline(int last) {
        this.lastTimeline = last;
    }

    @Override
    public int getLastTimeline() {
        return lastTimeline;
    }

    @Override
    public double getXd() {
        return xd;
    }

    @Override
    public double getYd() {
        return yd;
    }

    @Override
    public double getZd() {
        return zd;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public void setPosO(double x, double y, double z) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    /// @see MolangParticleInstance#setZRot(float)
    /// @deprecated
    public void setRoll(float roll) {
        this.roll = roll;
    }

    @Deprecated
    public float getRoll() {
        return roll;
    }

    @Override
    public void setColor(float red, float green, float blue, float alpha) {
        if (rCol != red || gCol != green || bCol != blue || this.alpha != alpha) {
            super.setColor(red, green, blue);
            super.setAlpha(alpha);

            this.abgr = FastColor.ABGR32.color(
                    Mth.floor(alpha * 255),
                    Mth.floor(bCol * 255),
                    Mth.floor(gCol * 255),
                    Mth.floor(rCol * 255)
            );
        }
    }

    @Override
    public void setCollision(boolean bool) {
        this.hasCollision = bool;
    }

    @Override
    public void discard() {
        remove();
    }

    @Override
    public boolean isDiscarded() {
        return removed;
    }

    @Override
    public VariableTable getVars() {
        return vars;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public double getRandom1() {
        return particleRandom1;
    }

    @Override
    public double getRandom2() {
        return particleRandom2;
    }

    @Override
    public double getRandom3() {
        return particleRandom3;
    }

    @Override
    public double getRandom4() {
        return particleRandom4;
    }

    @Override
    public ParticleEmitter getEmitter() {
        return emitter;
    }

    @Override
    public void tick() {
        super.tick();
        this.xRotO = xRot;
        this.yRotO = yRot;
        this.oRoll = roll;
        this.roll = roll + zRotD;
        this.renderSizeO = renderSize;
        for (IParticleComponent component : components) {
            component.update(this);
        }
        this.light = getLightColor();
    }

    /// @see TDParticle#render(ParticleBuffer, Camera, float)
    /// @deprecated
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {}

    private static final Matrix4f pose = new Matrix4f();
    private static final Quaternionf quat = new Quaternionf();

    public void render(ParticleBuffer buffer, Camera camera, float partialTick) {
        pose.identity();

        Vec3 cameraPos = camera.getPosition();
        pose.translate(
                (float) (Mth.lerp(partialTick, xo, x) - cameraPos.x()),
                (float) (Mth.lerp(partialTick, yo, y) - cameraPos.y()),
                (float) (Mth.lerp(partialTick, zo, z) - cameraPos.z())
        );

        if (preset.facingCameraMode != FaceCameraMode.DO_NOTHING) {
            preset.facingCameraMode.setRotation(this, quat, camera, partialTick);
            pose.rotate(quat);
        }
        if (xRot != 0 || yRot != 0 || roll != 0) {
            pose.rotateXYZ(
                    Mth.lerp(partialTick, xRotO, xRot),
                    Mth.lerp(partialTick, yRotO, yRot),
                    Mth.lerp(partialTick, oRoll, roll)
            );
        }

        float sx = Mth.lerp(partialTick, renderSizeO[0], renderSize[0]);
        float sy = Mth.lerp(partialTick, renderSizeO[1], renderSize[1]);
        float sz = Mth.lerp(partialTick, renderSizeO[2], renderSize[2]);
        pose.translate(sx * -0.5F, sy * -0.5F, sz * -0.5F);
        pose.scale(sx, sy, sz);

        renderer.render(this, pose, buffer);
    }

    @Override
    public void move(double x, double y, double z) {
        if (stoppedByCollision) return;
        double d0 = x;
        double d1 = y;
        double d2 = z;
        if (hasPhysics && hasCollision && (x != 0.0 || y != 0.0 || z != 0.0) && x * x + y * y + z * z < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            Vec3 vec3 = Entity.collideBoundingBox(null, new Vec3(x, y, z), getBoundingBox(), level, List.of());
            if (x != vec3.x) {
                this.xd = -Mth.sign(xd) * (Math.abs(xd) - collisionDrag) * coefficientOfRestitution;
            }
            if (y != vec3.y) {
                this.yd *= -coefficientOfRestitution;
            }
            if (z != vec3.z) {
                this.zd = -Mth.sign(zd) * (Math.abs(zd) - collisionDrag) * coefficientOfRestitution;
            }
            x = vec3.x;
            y = vec3.y;
            z = vec3.z;
        }

        if (x != 0.0 || y != 0.0 || z != 0.0) {
            moveDirectly(x, y, z);
        }

        if (Math.abs(d1) >= Mth.EPSILON && Math.abs(y) < Mth.EPSILON) {
            this.stoppedByCollision = true;
        }

        if (hasPhysics && hasCollision) {
            this.onGround = d1 != y && d1 < 0.0;
            boolean collided = d0 != x || d2 != z;

            if (onGround || collided) {
                if (!preset.collisionEvents.isEmpty()) {
                    for (ParticleMotionCollision.Event event : preset.collisionEvents) {
                        float tickSpeed = event.minSpeed() * getInvTickRate();
                        if (tickSpeed * tickSpeed < xd * xd + yd * yd + zd * zd) {
                            for (IEventNode node : preset.effect.events.get(event.event()).values()) {
                                node.execute(this);
                            }
                        }
                    }
                }
                if (expireOnContact) {
                    remove();
                }
            }
        }
    }

    @Override
    public void remove() {
        if (preset.lifeTimeEvents != null) {
            preset.lifeTimeEvents.onExpiration(this);
        }
        super.remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return TDPClient.TDP_RENDER_TYPE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return preset.environmentLighting ? super.getLightColor(partialTick) : FULL_LIGHT;
    }

    public short getLightColor() {
        if (preset.environmentLighting) {
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.hasChunkAt(pos)) {
                BlockState state = level.getBlockState(pos);
                if (state.emissiveRendering(level, pos)) {
                    return (short) 0xFF;
                }
                int i = level.getBrightness(LightLayer.SKY, pos);
                int j = level.getBrightness(LightLayer.BLOCK, pos);
                int k = state.getLightEmission(level, pos);
                if (j < k) {
                    j = k;
                }
                return (short) ((i << 4) | j);
            }
        }
        return (short) 0xFF;
    }

    @Override
    public Optional<ParticleGroup> getParticleGroup() {
        return Optional.ofNullable(particleGroup);
    }

    @Override
    public void setBoundingBox(AABB bb) {
        super.setBoundingBox(bb);
        this.renderBoundingBox = getBoundingBox().inflate(1.0);
    }
}
