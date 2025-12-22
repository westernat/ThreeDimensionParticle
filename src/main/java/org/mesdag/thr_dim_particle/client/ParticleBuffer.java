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
    public static final long U = COLOR + I + I;
    public static final long V = U + F;
    public static final long MODEL_LIGHT = V + F;
    public static final long MODEL_LIGHT_S = MODEL_LIGHT + S;
    public static final long ENV_LIGHT = MODEL_LIGHT + I;
    public static final long ENV_LIGHT_S = ENV_LIGHT + S;

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

    public static final SimpleData DATA = new SimpleData();

    public boolean storeMesh() {
        if (this.vertices == 0) {
            return false;
        }
        int vertices = this.vertices;
        this.vertices = 0;
        ByteBufferBuilder.Result result = buffer.build();
        if (result == null) {
            return false;
        }
        DATA.result = result;
        DATA.vertices = vertices;
        return true;
    }

    public static class SimpleData {
        private SimpleData() {}

        public ByteBufferBuilder.Result result;
        public int vertices;
    }
}
