package io.github.opencubicchunks.cubicchunks.mixin.core.client.world;

import io.github.opencubicchunks.cubicchunks.chunk.ClientChunkProviderCubeArray;
import io.github.opencubicchunks.cubicchunks.chunk.IClientCubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.client.ClientChunkProviderChunkArrayAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.client.IClientWorld;
import io.github.opencubicchunks.cubicchunks.world.lighting.IWorldLightManager;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.lighting.WorldLightManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ClientChunkProvider.class)
public abstract class MixinClientChunkProvider implements IClientCubeProvider {
    private volatile ClientChunkProviderCubeArray cubeArray;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ClientWorld world;
    private EmptyCube emptyCube;

    @Shadow private volatile ClientChunkProvider.ChunkArray array;

    @Shadow public abstract int getLoadedChunksCount();

    @Shadow public abstract WorldLightManager getLightManager();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(ClientWorld clientWorldIn, int viewDistance, CallbackInfo ci) {
        this.cubeArray = new ClientChunkProviderCubeArray(adjustCubeViewDistance(viewDistance), cube -> {});
        this.emptyCube = new EmptyCube(world);
    }

    private int adjustCubeViewDistance(int viewDistance) {
        return Math.max(2, Coords.sectionToCubeCeil(viewDistance)) + 3;
    }

    private static boolean isCubeValid(@Nullable BigCube cube, int x, int y, int z) {
        if (cube == null) {
            return false;
        }
        CubePos cubePos = cube.getCubePos();
        return cubePos.getX() == x && cubePos.getY() == y && cubePos.getZ() == z;
    }

    @Override
    public void unloadCube(int x, int y, int z) {
        if (!this.cubeArray.inView(x, y, z)) {
            return;
        }
        int index = this.cubeArray.getIndex(x, y, z);
        BigCube cube = this.cubeArray.get(index);
        if (isCubeValid(cube, x, y, z)) {
            // TODO: forge cube unload event
            // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload(chunk));
            this.cubeArray.unload(index, cube, null);
        }

    }

    @Nullable
    @Override
    public BigCube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load) {
        if (this.cubeArray.inView(cubeX, cubeY, cubeZ)) {
            BigCube chunk = this.cubeArray.get(this.cubeArray.getIndex(cubeX, cubeY, cubeZ));
            if (isCubeValid(chunk, cubeX, cubeY, cubeZ)) {
                return chunk;
            }
        }

        return load ? this.emptyCube : null;
    }

    @Override
    public BigCube loadCube(int cubeX, int cubeY, int cubeZ,
                            @Nullable CubeBiomeContainer biomes, PacketBuffer readBuffer, CompoundNBT nbtTagIn, boolean cubeExists) {

        if (!this.cubeArray.inView(cubeX, cubeY, cubeZ)) {
            LOGGER.warn("Ignoring cube since it's not in the view range: {}, {}, {}", cubeX, cubeY, cubeZ);
            return null;
        }
        int index = this.cubeArray.getIndex(cubeX, cubeY, cubeZ);
        BigCube cube = this.cubeArray.cubes.get(index);
        if (!isCubeValid(cube, cubeX, cubeY, cubeZ)) {
//             if (biomes == null) {
//                 LOGGER.warn("Ignoring chunk since we don't have complete data: {}, {}, {}", cubeX, cubeY, cubeZ);
//                 return null;
//             }

            cube = new BigCube(this.world, CubePos.of(cubeX, cubeY, cubeZ), biomes);
            cube.read(biomes, readBuffer, nbtTagIn, cubeExists);
            this.cubeArray.replace(index, cube);
        } else {
            cube.read(biomes, readBuffer, nbtTagIn, cubeExists);
        }

        WorldLightManager worldlightmanager = this.getLightManager();
        ((IWorldLightManager) worldlightmanager).enableLightSources(CubePos.of(cubeX, cubeY, cubeZ), true);

        ChunkSection[] cubeSections = cube.getCubeSections();
        for (int i = 0; i < cubeSections.length; ++i) {
            ChunkSection chunksection = cubeSections[i];
            worldlightmanager.updateSectionStatus(Coords.sectionPosByIndex(cube.getCubePos(), i), ChunkSection.isEmpty(chunksection));
        }

        ((IClientWorld)this.world).onCubeLoaded(cubeX, cubeY, cubeZ);
        // TODO: forge client cube load event
        // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(cube));
        return cube;
    }

    @Override public void setCenter(int sectionX, int sectionY, int sectionZ) {
        this.cubeArray.centerX = Coords.sectionToCube(sectionX);
        this.cubeArray.centerY = Coords.sectionToCube(sectionY);
        this.cubeArray.centerZ = Coords.sectionToCube(sectionZ);
    }

    @Inject(method = "setViewDistance", at = @At("HEAD"))
    private void setViewDistance(int viewDistance, CallbackInfo ci) {
        int old = this.cubeArray.viewDistance;
        int newDist = adjustCubeViewDistance(viewDistance);
        if (old == newDist) {
            return;
        }
        ClientChunkProviderCubeArray array = new ClientChunkProviderCubeArray(newDist, cube->{});
        array.centerX = this.cubeArray.centerX;
        array.centerY = this.cubeArray.centerY;
        array.centerZ = this.cubeArray.centerZ;

        for(int k = 0; k < this.cubeArray.cubes.length(); ++k) {
            BigCube chunk = this.cubeArray.cubes.get(k);
            if (chunk == null) {
                continue;
            }
            CubePos cubePos = chunk.getCubePos();
            if (array.inView(cubePos.getX(), cubePos.getY(), cubePos.getZ())) {
                array.replace(array.getIndex(cubePos.getX(), cubePos.getY(), cubePos.getZ()), chunk);
            }
        }

        this.cubeArray = array;
    }

    /**
     * @author Barteks2x
     * @reason Change the debug string
     */
    @Overwrite
    public String makeString() {
        //noinspection ConstantConditions
        return "Client Chunk Cache: " + ((ClientChunkProviderChunkArrayAccess) (Object) this.array).getChunks().length() + ", " + this.getLoadedChunksCount() +
                " | " + this.cubeArray.cubes.length() + ", " + getLoadedCubesCount();
    }

    public int getLoadedCubesCount() {
        return this.cubeArray.loaded;
    }
}