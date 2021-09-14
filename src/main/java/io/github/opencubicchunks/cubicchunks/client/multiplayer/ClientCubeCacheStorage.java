package io.github.opencubicchunks.cubicchunks.client.multiplayer;

import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import net.minecraft.client.multiplayer.ClientLevel;

public class ClientCubeCacheStorage {

    public final AtomicReferenceArray<LevelCube> cubes;
    public final int horizontalViewDistance;
    public final int verticalViewDistance;
    public volatile int centerX;
    public volatile int centerY;
    public volatile int centerZ;
    public int loaded;

    private final int horizontalSideLength;
    private final int verticalSideLength;
    private final ClientLevel world;
    private final int sideArea;

    public ClientCubeCacheStorage(int horizontalViewDistance, int verticalViewDistance, ClientLevel world) {
        this.horizontalViewDistance = horizontalViewDistance;
        this.verticalViewDistance = verticalViewDistance;
        this.horizontalSideLength = horizontalViewDistance * 2 + 1;
        this.verticalSideLength = verticalViewDistance * 2 + 1;
        this.world = world;
        this.sideArea = this.horizontalSideLength * this.horizontalSideLength;
        this.cubes = new AtomicReferenceArray<>(this.horizontalSideLength * this.verticalSideLength * this.horizontalSideLength);
    }

    public int getIndex(int x, int y, int z) {
        return Math.floorMod(y, this.verticalSideLength) * this.sideArea
            + Math.floorMod(z, this.horizontalSideLength) * this.horizontalSideLength
            + Math.floorMod(x, this.horizontalSideLength);
    }

    public void replace(int cubeIdx, @Nullable LevelCube chunkIn) {
        LevelCube cube = this.cubes.getAndSet(cubeIdx, chunkIn);
        if (cube != null) {
            --this.loaded;
            ((CubicClientLevel) this.world).onCubeUnload(cube);
        }

        if (chunkIn != null) {
            ++this.loaded;
        }

    }

    public LevelCube unload(int chunkIndex, LevelCube cube, @Nullable LevelCube replaceWith) {
        if (this.cubes.compareAndSet(chunkIndex, cube, replaceWith) && replaceWith == null) {
            --this.loaded;
        }
        ((CubicClientLevel) this.world).onCubeUnload(cube);
        return cube;
    }

    public boolean inView(int x, int y, int z) {
        return Math.abs(x - this.centerX) <= this.horizontalViewDistance
            && Math.abs(y - this.centerY) <= this.verticalViewDistance
            && Math.abs(z - this.centerZ) <= this.horizontalViewDistance;
    }

    @Nullable
    public LevelCube get(int chunkIndex) {
        return this.cubes.get(chunkIndex);
    }
}