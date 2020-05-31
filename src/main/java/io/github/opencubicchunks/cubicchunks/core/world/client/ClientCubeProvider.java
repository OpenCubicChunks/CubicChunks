package io.github.opencubicchunks.cubicchunks.core.world.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.access.ClientChunkProviderAccess;
import io.github.opencubicchunks.cubicchunks.core.world.Cube;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.lighting.WorldLightManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ClientCubeProvider extends ClientChunkProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ClientWorld world;
    private final CubeArray cubeArray;

    public ClientCubeProvider(ClientWorld world, int horizViewDistance, int vertViewDistance) {
        super(world, horizViewDistance);
        this.world = world;
        this.cubeArray = new CubeArray(adjustViewDistance(horizViewDistance), adjustViewDistance(vertViewDistance));
    }

    private static int adjustViewDistance(int dist) {
        return Math.max(2, dist) + 3;
    }

    @Nullable
    public ICube loadCube(int cubeX, int cubeY, int cubeZ,
                          @Nullable BiomeContainer biomes, //TODO: 3d biomes
                          PacketBuffer data, boolean hasData) {
        ClientChunkProviderAccess thisAccess = (ClientChunkProviderAccess) this;
        @SuppressWarnings("ConstantConditions") ClientChunkProviderAccess.ChunkArrayAccess arrayAccess =
                (ClientChunkProviderAccess.ChunkArrayAccess) (Object) thisAccess.getArray();
        if (!arrayAccess.invokeInView(cubeX, cubeZ)) {
            LOGGER.warn("Ignoring cue since it's not in the view range: {}, {}, {}", cubeX, cubeY, cubeZ);
            return null;
        }
        int columnIdx = arrayAccess.invokeGetIndex(cubeX, cubeZ);
        Chunk chunk = arrayAccess.getChunks().get(columnIdx);
        if (!isValidColumn(chunk, cubeX, cubeZ)) {
            LOGGER.warn("Ignoring cube since we don't have column data: {}, {}, {}", cubeX, cubeY, cubeZ);
            return null;

        }
        int cubeIdx = cubeArray.getIndex(cubeX, cubeY, cubeZ);
        Cube cube = (Cube) cubeArray.get(cubeIdx);
        SectionPos cubePos = SectionPos.of(cubeX, cubeY, cubeZ);

        if (!isValidCube(cube, cubeX, cubeY, cubeZ)) {
            // TODO: biomes and reject if no biome data
            cube = new Cube(this.world, cubePos);
            cube.read(biomes, data, hasData);
            cubeArray.setCube(columnIdx, cube);
        } else {
            cube.read(biomes, data, hasData);
        }

        ChunkSection section = cube.getStorage();
        WorldLightManager worldlightmanager = this.getLightManager();
        // TODO: what is this *actually* doing?
        // worldlightmanager.enableLightSources(new ChunkPos(cubeX, cubeZ), true);

        worldlightmanager.updateSectionStatus(cubePos, ChunkSection.isEmpty(section));

        ((ICubicWorldInternal) this.world).onCubeLoad(cubeX, cubeY, cubeZ);
        // TODO API events
        // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
        return cube;
    }

    private static boolean isValidColumn(@Nullable Chunk chunkIn, int x, int z) {
        if (chunkIn == null) {
            return false;
        } else {
            ChunkPos chunkpos = chunkIn.getPos();
            return chunkpos.x == x && chunkpos.z == z;
        }
    }


    private static boolean isValidCube(@Nullable ICube cube, int cubeX, int cubeY, int cubeZ) {
        if (cube == null) {
            return false;
        } else {
            return cube.getX() == cubeX && cube.getY() == cubeY && cube.getZ() == cubeZ;
        }
    }

    @Nullable
    public Chunk loadChunk(int chunkX, int chunkZ, @Nullable BiomeContainer biomeContainerIn, PacketBuffer packetIn, CompoundNBT nbtTagIn, int sizeIn) {
        throw new UnsupportedOperationException("Cannot load Chunks in cubic chunks world")
    }

    // TEMPORARY: for early testing purposes, while cubes exist, there is no real vertical aspect to it
    // they just make 16-tall columns and everything else is ignored
    public final class CubeArray {
        private final AtomicReferenceArray<ICube> cubes;
        private final int horizViewDistance;
        private final int vertViewDistance;
        private final int horizSize;
        private final int vertSize;
        private volatile int xCenter;
        private volatile int zCenter;
        private int loadedCount;

        private CubeArray(int horizontalViewDistance, int verticalViewDistance) {
            this.horizViewDistance = horizontalViewDistance;
            this.horizSize = horizontalViewDistance * 2 + 1;
            this.vertViewDistance = verticalViewDistance;
            this.vertSize = 16; //verticalViewDistance * 2 + 1;
            this.cubes = new AtomicReferenceArray<>(this.horizSize * this.horizSize * this.vertSize);
        }

        private int getIndex(int x, int y, int z) {
            return Math.floorMod(z, this.horizSize) * this.horizSize * this.horizSize +
                    Math.floorMod(x, this.horizSize) * this.horizSize +
                    Math.floorMod(y, this.vertSize);
        }

        protected void setCube(int idx, @Nullable ICube cube) {
            ICube oldCube = this.cubes.getAndSet(idx, cube);
            if (oldCube != null) {
                this.loadedCount--;
                ((ICubicWorldInternal.Client) world).onCubeUnloaded(oldCube);
            }
            if (cube != null) {
                this.loadedCount++;
            }
        }

        protected ICube unloadCube(int idx, ICube cube) {
            if (this.cubes.compareAndSet(idx, cube, null)) {
                --this.loadedCount;
            }
            ((ICubicWorldInternal.Client) world).onCubeUnloaded(cube);
            return cube;
        }

        private boolean isInView(int x, int y, int z) {
            return Math.abs(x - this.xCenter) <= this.horizViewDistance &&
                    Math.abs(z - this.zCenter) <= this.horizViewDistance &&
                    y >= 0 && y < 16;
        }

        @Nullable
        protected ICube get(int idx) {
            return this.cubes.get(idx);
        }
    }
}
