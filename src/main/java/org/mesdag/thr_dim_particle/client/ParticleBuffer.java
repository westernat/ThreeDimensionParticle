package org.mesdag.thr_dim_particle.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import javax.annotation.Nullable;

public class ParticleBuffer {
    public int vertices;
    public final ByteBufferBuilder buffer;

    public ParticleBuffer() {
        this.buffer = Tesselator.getInstance().buffer;
    }

    @Nullable
    public MeshData storeMesh() {
        if (vertices == 0) {
            return null;
        }
        ByteBufferBuilder.Result result = buffer.build();
        if (result == null) {
            return null;
        }
        int count = VertexFormat.Mode.QUADS.indexCount(vertices);
        VertexFormat.IndexType indexType = VertexFormat.IndexType.least(vertices);
        return new MeshData(result, new MeshData.DrawState(TDPRenderType.FORMAT, vertices, count, VertexFormat.Mode.QUADS, indexType));
    }
}
