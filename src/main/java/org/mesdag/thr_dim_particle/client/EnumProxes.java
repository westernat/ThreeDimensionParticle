package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import org.lwjgl.opengl.GL30;

public final class EnumProxes {
    public static final EnumProxy<VertexFormatElement.Usage> LIGHT = new EnumProxy<>(
            VertexFormatElement.Usage.class,
            "thr_dim_particle:light",
            (VertexFormatElement.Usage.SetupState) (size, type, stride, pointer, index) -> {
                GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
            });
}
