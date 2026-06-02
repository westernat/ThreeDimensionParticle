package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;

/// @see com.mojang.blaze3d.vertex.BufferBuilder
public class ParticleBuffer {
    protected static final byte B = 1;
    protected static final byte Z = 1;
    protected static final byte S = 2;
    protected static final byte I = 4;
    protected static final byte F = 4;
    protected static final byte J = 8;
    protected static final byte D = 8;

    public static final int POS_X = 0;
    public static final int POS_Y = POS_X + F;
    public static final int POS_Z = POS_Y + F;
    public static final int COLOR = POS_Z + F;
    public static final int UV = COLOR + I + I;
    public static final int LIGHT = UV + F + F;

    public final BufferBuilder buffer;
    protected int lastPtr;

    public ParticleBuffer(BufferBuilder buffer) {
        this.buffer = buffer;
    }

    public void begin() {
        buffer.begin(VertexFormat.Mode.QUADS, TDPRenderType.FORMAT);
    }

    public int pushPtr(int bytes) {
        int capacity = buffer.buffer.capacity();
        if (buffer.nextElementByte + bytes > capacity) {
            int i = Math.min(capacity, 2097152);
            int j = Math.max(capacity + i, bytes);
            if (j >= 2097152) {
                return -1;
            }
            buffer.buffer = MemoryTracker.resize(buffer.buffer, j);
            buffer.buffer.rewind();
        }
        return this.lastPtr = buffer.buffer.position() + buffer.nextElementByte;
    }

    public void popPtr(int currentPtr) {
        int bytes = currentPtr - lastPtr;
        buffer.vertices += bytes / TDPRenderType.VERTEX_SIZE;
        buffer.nextElementByte += bytes;
    }

    public @Nullable BufferBuilder end() {
        if (buffer.vertices <= 0) {
            buffer.reset();
            return null;
        }
        return buffer;
    }
}
