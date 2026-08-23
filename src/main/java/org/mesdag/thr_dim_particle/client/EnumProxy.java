package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL30;

@ApiStatus.Internal
public final class EnumProxy<E extends Enum<E>> {
    private E value;

    private EnumProxy() {}

    public void setValue(E value) {
        this.value = value;
    }

    public E getValue() {
        return value;
    }

    public static final EnumProxy<VertexFormatElement.Usage> LIGHT = new EnumProxy<>();
    public static final String LIGHT_NAME = "thr_dim_particle:light";
    public static final VertexFormatElement.Usage.ClearState LIGHT_CLEAR = (index, elementIndex) -> {
        GlStateManager._disableVertexAttribArray(elementIndex);
    };
    public static final VertexFormatElement.Usage.SetupState LIGHT_SETUP = (count, glType, stride, offset, index, stateIndex) -> {
        GlStateManager._enableVertexAttribArray(stateIndex);
        GL30.glVertexAttribIPointer(stateIndex, count, glType, stride, offset);
    };
}
