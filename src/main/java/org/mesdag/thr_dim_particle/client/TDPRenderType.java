package org.mesdag.thr_dim_particle.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.mesdag.thr_dim_particle.client.TDPClient.*;

public final class TDPRenderType extends RenderType.CompositeRenderType {
    public static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    static final TextureStateShard NO_MIPMAP_TEXTURE = new TextureStateShard(TDPClient.ATLAS_LOCATION, false, false);
    static final TransparencyStateShard SKIP_TRANSPARENCY = new TransparencyStateShard("skip_transparency", () -> {}, () -> {});
    static final VertexFormatElement COLOR1 = new VertexFormatElement(0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    static final VertexFormatElement LIGHT = new VertexFormatElement(0, VertexFormatElement.Type.USHORT, EnumProxy.LIGHT.getValue(), 1);
    static final VertexFormat FORMAT = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
            .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
            .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
            .put("Color1", COLOR1) // 用于正片叠底
            .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
            .put("Light", LIGHT) // 传入msl, mbl, esl, ebl
            .build());
    //    public static final int VERTEX_SIZE = FORMAT.getVertexSize();
    public static final int VERTEX_SIZE = 30;
    private static final TDPRenderType[] TYPES = new TDPRenderType[]{
            new TDPRenderType(0, "tdp_particle_solid", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleSolidShaderInstance))
                            .setTextureState(NO_MIPMAP_TEXTURE)
                            .setTransparencyState(SKIP_TRANSPARENCY) // 渲染时提前打开
                            .createCompositeState(false)
            ),
            new TDPRenderType(1,
                    "tdp_particle_cutout", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutShaderInstance))
                            .setTextureState(NO_MIPMAP_TEXTURE)
                            .setTransparencyState(SKIP_TRANSPARENCY)
                            .createCompositeState(false)
            ),
            new TDPRenderType(2,
                    "tdp_particle_cutout_mipped", FORMAT, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleCutoutMippedShaderInstance))
                            .setTextureState(new RenderStateShard.TextureStateShard(ATLAS_LOCATION, false, true))
                            .setTransparencyState(SKIP_TRANSPARENCY)
                            .createCompositeState(false)
            ),
            new TDPRenderType(3,
                    "tdp_particle_translucent", FORMAT, VertexFormat.Mode.QUADS, 256, true, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> particleTranslucentShaderInstance))
                            .setTextureState(NO_MIPMAP_TEXTURE)
                            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
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

    private TDPRenderType(int index, String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, CompositeState state) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, state);
        this.index = index;
    }

    public static TDPRenderType get(int index) {
        return TYPES[index];
    }

    public static TDPRenderType get(String id) {
        return MAP.get(id);
    }
}
