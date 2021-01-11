package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.LightHeightmapGetter;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.LightSurfaceTrackerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.SectionLightStorageAccess;
import io.github.opencubicchunks.cubicchunks.world.lighting.ICubicSkyLightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.ISkyLightColumnChecker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.SkyLightEngine;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkyLightEngine.class)
public abstract class MixinSkyLightEngine extends MixinLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> implements ISkyLightColumnChecker,
    ICubicSkyLightEngine {
    /**
     * @author CursedFlames
     * @reason disable vanilla sky light logic
     */
    @Overwrite
    protected void checkNode(long id) {
        super.checkNode(id);
    }

    @Override public void checkSkyLightColumn(int x, int z, int oldHeight, int newHeight) {
        ((SectionLightStorageAccess) this.storage).invokeRunAllUpdates();
        if (oldHeight > newHeight) {
            // not sure if this is necessary - also maybe it should be done inside the loop? not sure if threaded stuff can result in storage becoming out of date inside the loop
            ((SectionLightStorageAccess) this.storage).invokeRunAllUpdates();

            for (int y = oldHeight-1; y >= newHeight; y--) {
                long pos = new BlockPos(x, y, z).asLong();
                if (((SectionLightStorageAccess) this.storage).invokeStoringLightForSection(SectionPos.blockToSection(pos))) {
                    addEmissionAtPos(pos);
                }
            }
        } else {
            for (int y = oldHeight; y < newHeight; y++) {
                long pos = new BlockPos(x, y, z).asLong();
                // Don't need to check storing light for pos here, since it's already handled by checkNode
                this.checkNode(pos);
            }
        }
    }

    private void addEmissionAtPos(long pos) {
        this.checkEdge(Long.MAX_VALUE, pos, 0, true);
    }

    @Inject(method = "getComputedLevel(JJI)I",
        at = @At("HEAD"), cancellable = true)
    private void onGetComputedLevel(long id, long excludedId, int maxLevel, CallbackInfoReturnable<Integer> cir) {
        if (this.chunkSource.getLevel() instanceof ClientLevel) return;
        BlockPos pos = BlockPos.of(id);
        // FIXME this may or may not be a horrendously unsafe way of getting the light heightmap. I'm not sure.
        BlockGetter chunk = this.chunkSource.getChunkForLighting(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) return;
        LightSurfaceTrackerWrapper heightmap = ((LightHeightmapGetter) chunk).getLightHeightmap();
        int height = heightmap.getFirstAvailable(pos.getX(), pos.getZ());
        if (height <= pos.getY()) {
            cir.setReturnValue(0);
        }
    }

    @Override
    public void doSkyLightForCube(IBigCube cube) {
        CubePos cubePos = cube.getCubePos();
        ChunkPos chunkPos = cubePos.asChunkPos();
        int minY = cubePos.minCubeY();
        int maxY = cubePos.maxCubeY();
        for (int sectionX = 0; sectionX < IBigCube.DIAMETER_IN_SECTIONS; sectionX++) {
            for (int sectionZ = 0; sectionZ < IBigCube.DIAMETER_IN_SECTIONS; sectionZ++) {
                // FIXME possibly unsafe light heightmap access here too
                BlockGetter chunk = this.chunkSource.getChunkForLighting(chunkPos.x + sectionX, chunkPos.z + sectionZ);
                LightSurfaceTrackerWrapper heightmap = ((LightHeightmapGetter) chunk).getLightHeightmap();
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int height = heightmap.getFirstAvailable(x, z);
                        if (height <= maxY) {
                            height = Math.max(height, minY);
                            for (int y = maxY; y >= height; y--) {
                                long pos = new BlockPos((chunkPos.x + sectionX)*16 + x, y, (chunkPos.z + sectionZ)*16 + z).asLong();
                                // Not sure if this is necessary
                                ((SectionLightStorageAccess) this.storage).invokeRunAllUpdates();

                                if (((SectionLightStorageAccess) this.storage).invokeStoringLightForSection(SectionPos.blockToSection(pos))) {
                                    addEmissionAtPos(pos);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
