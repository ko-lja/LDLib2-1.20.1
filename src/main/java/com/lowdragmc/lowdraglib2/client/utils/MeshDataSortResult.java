package com.lowdragmc.lowdraglib2.client.utils;

import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MeshDataSortResult {
    public final Vector3f[] centroids;
    public final VertexFormat.IndexType indexType;
    public final VertexFormat.Mode mode;
    public final ByteBuffer indexBuffer;

    public MeshDataSortResult(Vector3f[] centroids, VertexFormat.IndexType indexType, VertexFormat.Mode mode) {
        this(centroids, indexType, mode, null);
    }

    public MeshDataSortResult(Vector3f[] centroids, VertexFormat.IndexType indexType,
                              VertexFormat.Mode mode, ByteBuffer indexBuffer) {
        this.centroids = centroids;
        this.indexType = indexType;
        this.mode = mode;
        this.indexBuffer = indexBuffer;
    }

    @Deprecated
    public void applyTo(BufferBuilder.RenderedBuffer meshData) {
        throw new UnsupportedOperationException("MeshData does not exist on 1.20.1; RenderedBuffer has no replaceable index buffer");
    }

    @Nullable
    public ByteBuffer buildSortedIndexBuffer(VertexSorting sorting) {
        int[] primitiveIndices = sorting.sort(this.centroids);
        
        return switch (mode) {
            case TRIANGLES -> buildTriangleIndexBuffer(primitiveIndices);
            case QUADS -> buildQuadIndexBuffer(primitiveIndices);
            case LINES, DEBUG_LINES -> buildLineIndexBuffer(primitiveIndices);
            default -> null;
        };
    }
    
    private ByteBuffer buildTriangleIndexBuffer(int[] primitiveIndices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(primitiveIndices.length * 3 * this.indexType.bytes).order(ByteOrder.nativeOrder());
        IntConsumer indexWriter = createIndexWriter(buffer, this.indexType);
        
        for (int primitiveIndex : primitiveIndices) {
            int baseVertexIndex = primitiveIndex * 3;
            indexWriter.accept(baseVertexIndex);
            indexWriter.accept(baseVertexIndex + 1);
            indexWriter.accept(baseVertexIndex + 2);
        }
        
        buffer.flip();
        return buffer;
    }
    
    private ByteBuffer buildQuadIndexBuffer(int[] primitiveIndices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(primitiveIndices.length * 6 * this.indexType.bytes).order(ByteOrder.nativeOrder());
        IntConsumer indexWriter = createIndexWriter(buffer, this.indexType);
        
        for (int primitiveIndex : primitiveIndices) {
            int baseVertexIndex = primitiveIndex * 4;
            // 两个三角形：(0,1,2) 和 (2,3,0)
            indexWriter.accept(baseVertexIndex);
            indexWriter.accept(baseVertexIndex + 1);
            indexWriter.accept(baseVertexIndex + 2);
            indexWriter.accept(baseVertexIndex + 2);
            indexWriter.accept(baseVertexIndex + 3);
            indexWriter.accept(baseVertexIndex);
        }
        
        buffer.flip();
        return buffer;
    }
    
    private ByteBuffer buildLineIndexBuffer(int[] primitiveIndices) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(primitiveIndices.length * 2 * this.indexType.bytes).order(ByteOrder.nativeOrder());
        IntConsumer indexWriter = createIndexWriter(buffer, this.indexType);
        
        for (int primitiveIndex : primitiveIndices) {
            int baseVertexIndex = primitiveIndex * 2;
            indexWriter.accept(baseVertexIndex);
            indexWriter.accept(baseVertexIndex + 1);
        }
        
        buffer.flip();
        return buffer;
    }
    
    private IntConsumer createIndexWriter(ByteBuffer buffer, VertexFormat.IndexType type) {
        return switch (type) {
            case SHORT -> value -> buffer.putShort((short) value);
            case INT -> value -> buffer.putInt(value);
        };
    }
}