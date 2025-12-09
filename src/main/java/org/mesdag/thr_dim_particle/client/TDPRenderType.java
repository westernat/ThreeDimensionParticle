package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.thr_dim_particle.TDP;

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
    private static final ResourceLocation ATLAS = TextureAtlas.LOCATION_BLOCKS; // todo 换particle图集
    private static final TDPRenderType[] TYPES = new TDPRenderType[]{
            new TDPRenderType(0, "tdp_particle_solid", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleSolidShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            ),
            new TDPRenderType(1,
                    "tdp_particle_cutout", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, false))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            ),
            new TDPRenderType(2,
                    "tdp_particle_cutout_mipped", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutMippedShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, true))
                            .setTransparencyState(RenderType.NO_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            ),
            new TDPRenderType(3,
                    "tdp_particle_translucent", FORMAT, VertexFormat.Mode.QUADS, 256, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleTranslucentShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS, false, false))
                            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                            .setOverlayState(RenderType.OVERLAY)
                            .setLightmapState(RenderType.LIGHTMAP)
                            .createCompositeState(false)
            )
    };
    private static final Map<ResourceLocation, TDPRenderType> MAP = Util.make(new HashMap<>(), map -> {
        map.put(TDP.asResource("solid"), TYPES[0]);
        map.put(TDP.asResource("cutout"), TYPES[1]);
        map.put(TDP.asResource("cutout_mipped"), TYPES[2]);
        map.put(TDP.asResource("translucent"), TYPES[3]);
    });

    public final int index;

    private TDPRenderType(int index, String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, CompositeState state) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, state);
        this.index = index;
    }

    public static TDPRenderType get(int index) {
        return TYPES[index];
    }

    public static TDPRenderType get(ResourceLocation id) {
        return MAP.get(id);
    }
}
