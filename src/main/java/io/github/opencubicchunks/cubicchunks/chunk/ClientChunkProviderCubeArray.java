package io.github.opencubicchunks.cubicchunks.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class ClientChunkProviderCubeArray {

    public final AtomicReferenceArray<BigCube> cubes;
    public final int viewDistance;
    private final int sideLength;
    private final int sideArea;
    private final Consumer<BigCube> onUnload;
    public volatile int centerX;
    public volatile int centerY;
    public volatile int centerZ;
    public int loaded;

    public ClientChunkProviderCubeArray(int viewDistanceIn, Consumer<BigCube> onUnload) {
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

    public void replace(int chunkIndex, @Nullable BigCube chunkIn) {
        BigCube chunk = this.cubes.getAndSet(chunkIndex, chunkIn);
        if (chunk != null) {
            --this.loaded;
            onUnload.accept(chunk);
        }

        if (chunkIn != null) {
            ++this.loaded;
        }

    }

    public BigCube unload(int chunkIndex, BigCube chunkIn, @Nullable BigCube replaceWith) {
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
    public BigCube get(int chunkIndex) {
        return this.cubes.get(chunkIndex);
    }
}