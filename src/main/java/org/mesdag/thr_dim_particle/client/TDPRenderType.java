package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.mesdag.thr_dim_particle.client.TDPClient.*;

public final class TDPRenderType extends RenderType.CompositeRenderType {
    static final VertexFormatElement COLOR1 = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    static final VertexFormat FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("Color1", COLOR1) // 用于正片叠底
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1) // 实际传入的值为模型光照uv2
            .add("UV2", VertexFormatElement.UV2)
            .build();
//    public static final int VERTEX_SIZE = FORMAT.getVertexSize();
    public static final int VERTEX_SIZE = 36;
    private static final TDPRenderType[] TYPES = new TDPRenderType[]{
            new TDPRenderType(0, "tdp_particle_solid", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleSolidShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS_LOCATION, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            ),
            new TDPRenderType(1,
                    "tdp_particle_cutout", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS_LOCATION, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            ),
            new TDPRenderType(2,
                    "tdp_particle_cutout_mipped", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutMippedShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS_LOCATION, false, true))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            ),
            new TDPRenderType(3,
                    "tdp_particle_translucent", FORMAT, VertexFormat.Mode.QUADS, 256, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleTranslucentShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS_LOCATION, false, false))
                            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            )
    };
    private static final Map<String, TDPRenderType> MAP = Util.make(new HashMap<>(), map -> {
        map.put("solid", TYPES[0]);
        map.put("cutout", TYPES[1]);
        map.put("cutout_mipped", TYPES[2]);
        map.put("translucent", TYPES[3]);
    });

    public final int index;
    private final CompositeState state;

    private TDPRenderType(int index, String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, CompositeState state) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, state);
        this.index = index;
        this.state = state;
    }

    @SuppressWarnings("all")
    public void draw() {
        // region net.minecraft.client.renderer.RenderStateShard.setupRenderState
        for (RenderStateShard shard : state.states) {
            shard.setupState.run();
        }
        // endregion

        // region com.mojang.blaze3d.vertex.BufferUploader._drawWithShader
        //      region com.mojang.blaze3d.vertex.BufferUploader.upload
        ParticleBuffer.SimpleData data = ParticleBuffer.DATA;
        ByteBuffer bb = data.result.byteBuffer();
        VertexBuffer vb = FORMAT.immediateDrawVertexBuffer;
        if (vb == null) {
            FORMAT.immediateDrawVertexBuffer = vb = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            vb.mode = VertexFormat.Mode.QUADS;
        }
        if (vb != BufferUploader.lastImmediateBuffer) {
            GlStateManager._glBindVertexArray(vb.arrayObjectId);
            BufferUploader.lastImmediateBuffer = vb;
        }
        //          region com.mojang.blaze3d.vertex.VertexBuffer.upload
        try {
            GL15.glBindBuffer(34962, vb.vertexBufferId);
            if (vb.format == null) {
                FORMAT._setupBufferState();
                vb.format = FORMAT;
            }
            GL15.glBufferData(34962, bb, 35048);

            int indexCount = data.vertices / 4 * 6;
            RenderSystem.AutoStorageIndexBuffer asib = RenderSystem.sharedSequentialQuad;
            if (asib != vb.sequentialIndices || !asib.hasStorage(indexCount)) {
                asib.bind(indexCount);
            }
            vb.sequentialIndices = asib;
            vb.indexCount = indexCount;
            vb.indexType = (data.vertices & -65536) == 0 ? VertexFormat.IndexType.SHORT : VertexFormat.IndexType.INT;
        } catch (Throwable throwable1) {
            try {
                data.result.close();
            } catch (Throwable throwable) {
                throwable1.addSuppressed(throwable);
            }
            throw throwable1;
        }
        data.result.close();
        //          endregion
        //      endregion

        //      region com.mojang.blaze3d.vertex.VertexBuffer._drawWithShader
        ShaderInstance shader = RenderSystem.shader;
        //          region net.minecraft.client.renderer.ShaderInstance.setDefaultUniforms
        shader.MODEL_VIEW_MATRIX.set(RenderSystem.modelViewMatrix);
        shader.PROJECTION_MATRIX.set(RenderSystem.projectionMatrix);
        shader.COLOR_MODULATOR.set(RenderSystem.shaderColor);
        shader.FOG_START.set(RenderSystem.shaderFogStart);
        shader.FOG_END.set(RenderSystem.shaderFogEnd);
        shader.FOG_COLOR.set(RenderSystem.shaderFogColor);
        shader.FOG_SHAPE.set(RenderSystem.shaderFogShape.index);
        //          endregion
        //          region net.minecraft.client.renderer.ShaderInstance apply()V
        ShaderInstance.lastAppliedShader = shader;
        int id = shader.programId;
        if (id != ShaderInstance.lastProgramId) {
            GL20.glUseProgram(id);
            ShaderInstance.lastProgramId = id;
        }
        int i = GlStateManager._getActiveTexture();

        GL20.glUniform1i(GL20.glGetUniformLocation(id, "Sampler0"), 0);
        GL13.glActiveTexture(33984);
        int l = RenderSystem.shaderTextures[0];
        GlStateManager.TextureState texture = GlStateManager.TEXTURES[0];
        if (l != texture.binding) {
            texture.binding = l;
            GL11.glBindTexture(3553, l);
        }

        GL20.glUniform1i(GL20.glGetUniformLocation(id, "Sampler2"), 1);
        GL13.glActiveTexture(33984 + 1);
        l = RenderSystem.shaderTextures[2];
        texture = GlStateManager.TEXTURES[1];
        if (l != texture.binding) {
            texture.binding = l;
            GL11.glBindTexture(3553, l);
        }

        GL13.glActiveTexture(i);

        shader.MODEL_VIEW_MATRIX.upload();
        shader.PROJECTION_MATRIX.upload();
        shader.COLOR_MODULATOR.upload();
        shader.FOG_START.upload();
        shader.FOG_END.upload();
        shader.FOG_COLOR.upload();
        shader.FOG_SHAPE.upload();
        //          endregion
        //          region com.mojang.blaze3d.vertex.VertexBuffer.draw
        GL11.glDrawElements(4, vb.indexCount, (vb.sequentialIndices == null ? vb.indexType : vb.sequentialIndices.type()).asGLType, 0L);
        //          endregion
        //          region net.minecraft.client.renderer.ShaderInstance.clear
        GL20.glUseProgram(0);
        ShaderInstance.lastProgramId = -1;
        ShaderInstance.lastAppliedShader = null;
        i = GlStateManager._getActiveTexture();

        GL13.glActiveTexture(33984);
        texture = GlStateManager.TEXTURES[0];
        if (0 != texture.binding) {
            texture.binding = 0;
            GL11.glBindTexture(3553, 0);
        }

        GL13.glActiveTexture(33984 + 1);
        texture = GlStateManager.TEXTURES[1];
        if (0 != texture.binding) {
            texture.binding = 0;
            GL11.glBindTexture(3553, 0);
        }

        GL13.glActiveTexture(i);
        //          endregion
        //      endregion
        // endregion

        // region net.minecraft.client.renderer.RenderStateShard.clearRenderState
        for (RenderStateShard shard : state.states) {
            shard.clearState.run();
        }
        // endregion
    }

    public static TDPRenderType get(int index) {
        return TYPES[index];
    }

    public static TDPRenderType get(String id) {
        return MAP.get(id);
    }
}
