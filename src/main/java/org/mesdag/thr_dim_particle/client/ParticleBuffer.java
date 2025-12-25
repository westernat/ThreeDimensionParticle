package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;

/// @see com.mojang.blaze3d.vertex.BufferBuilder
public class ParticleBuffer {
    protected static final byte B = 1;
    protected static final byte Z = 1;
    protected static final byte S = 2;
    protected static final byte I = 4;
    protected static final byte F = 4;
    protected static final byte J = 8;
    protected static final byte D = 8;

    public static final long POS_X = 0;
    public static final long POS_Y = POS_X + F;
    public static final long POS_Z = POS_Y + F;
    public static final long COLOR = POS_Z + F;
    public static final long UV = COLOR + I + I;
    public static final long LIGHT = UV + F + F;

    protected int vertices;
    protected final ByteBufferBuilder buffer;
    protected long lastPtr;

    public ParticleBuffer(ByteBufferBuilder buffer) {
        this.buffer = buffer;
    }

    public long pushPtr(int bytes) {
        buffer.ensureCapacity(buffer.writeOffset + bytes);
        return this.lastPtr = buffer.pointer + buffer.writeOffset;
    }

    public void popPtr(long currentPtr) {
        int bytes = (int) (currentPtr - lastPtr);
        this.vertices += bytes / TDPRenderType.VERTEX_SIZE;
        buffer.writeOffset += bytes;
    }

    public long reserve() {
        ++this.vertices;
        return buffer.reserve(TDPRenderType.VERTEX_SIZE);
    }

    public void storeMesh(TDPRenderType renderType) {
        if (this.vertices == 0) {
            return;
        }
        int vertices = this.vertices;
        this.vertices = 0;

        int offset = buffer.nextResultOffset;
        int capacity = buffer.writeOffset - offset;
        if (capacity == 0) {
            return;
        }
        buffer.nextResultOffset = buffer.writeOffset;
        buffer.resultCount++;

        renderType.draw(buffer.pointer + offset, capacity, vertices, buffer::freeResult);
    }
}
