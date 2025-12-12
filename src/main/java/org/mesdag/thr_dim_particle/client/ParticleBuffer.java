package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import javax.annotation.Nullable;

public class ParticleBuffer {
    public static final byte B = 1;
    public static final byte Z = 1;
    public static final byte S = 2;
    public static final byte I = 4;
    public static final byte F = 4;
    public static final byte J = 8;

    public static final long POS_X = 0;
    public static final long POS_Y = POS_X + F;
    public static final long POS_Z = POS_Y + F;
    public static final long COLOR = POS_Z + F;
    public static final long U = COLOR + I + I;
    public static final long V = U + F;
    public static final long MODEL_LIGHT = V + F;
    public static final long ENV_LIGHT = MODEL_LIGHT + I;

    protected int vertices;
    protected final ByteBufferBuilder buffer;

    public ParticleBuffer(ByteBufferBuilder buffer) {
        this.buffer = buffer;
    }

    public long reserve() {
        this.vertices++;
        return buffer.reserve(TDPRenderType.VERTEX_SIZE);
    }

    @Nullable
    public MeshData storeMesh() {
        if (this.vertices == 0) {
            return null;
        }
        int vertices = this.vertices;
        this.vertices = 0;
        ByteBufferBuilder.Result result = buffer.build();
        if (result == null) {
            return null;
        }
        int count = VertexFormat.Mode.QUADS.indexCount(vertices);
        VertexFormat.IndexType indexType = VertexFormat.IndexType.least(vertices);
        return new MeshData(result, new MeshData.DrawState(TDPRenderType.FORMAT, vertices, count, VertexFormat.Mode.QUADS, indexType));
    }
}
