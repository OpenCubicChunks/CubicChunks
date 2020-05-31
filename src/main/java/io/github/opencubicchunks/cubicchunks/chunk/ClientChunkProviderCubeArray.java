package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class ClientChunkProviderCubeArray {

    public final AtomicReferenceArray<Cube> cubes;
    public final int viewDistance;
    private final int sideLength;
    private final int sideArea;
    private final Consumer<Cube> onUnload;
    public volatile int centerX;
    public volatile int centerY;
    public volatile int centerZ;
    public int loaded;

    public ClientChunkProviderCubeArray(int viewDistanceIn, Consumer<Cube> onUnload) {
        this.viewDistance = viewDistanceIn;
        this.sideLength = viewDistanceIn * 2 + 1;
        this.sideArea = this.sideLength * this.sideLength;
        this.onUnload = onUnload;
        this.cubes = new AtomicReferenceArray<>(this.sideLength * this.sideLength * this.sideLength);
    }

    public int getIndex(int x, int y, int z) {
        return Math.floorMod(z, this.sideLength) * this.sideArea
                + Math.floorMod(y, this.sideLength) * this.sideLength
                + Math.floorMod(x, this.sideLength);
    }

    public void replace(int chunkIndex, @Nullable Cube chunkIn) {
        Cube chunk = this.cubes.getAndSet(chunkIndex, chunkIn);
        if (chunk != null) {
            --this.loaded;
            onUnload.accept(chunk);
        }

        if (chunkIn != null) {
            ++this.loaded;
        }

    }

    public Cube unload(int chunkIndex, Cube chunkIn, @Nullable Cube replaceWith) {
        if (this.cubes.compareAndSet(chunkIndex, chunkIn, replaceWith) && replaceWith == null) {
            --this.loaded;
        }

        onUnload.accept(chunkIn);
        return chunkIn;
    }

    public boolean inView(int x, int y, int z) {
        return Math.abs(x - this.centerX) <= this.viewDistance
                && Math.abs(y - this.centerY) <= this.viewDistance
                && Math.abs(z - this.centerZ) <= this.viewDistance;
    }

    @Nullable
    public Cube get(int chunkIndex) {
        return this.cubes.get(chunkIndex);
    }
}