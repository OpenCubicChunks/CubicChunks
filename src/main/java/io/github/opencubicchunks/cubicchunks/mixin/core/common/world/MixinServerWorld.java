package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.SurfaceTrackerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld extends Level implements IServerWorld {
    protected MixinServerWorld(WritableLevelData p_i231617_1_, ResourceKey<Level> p_i231617_2_, DimensionType p_i231617_4_,
            Supplier<ProfilerFiller> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
    }

    @Override
    public void onCubeUnloading(BigCube cube) {
        cube.invalidateAllBlockEntities();

        ChunkPos pos = cube.getCubePos().asChunkPos();
        for (int x = 0; x < IBigCube.DIAMETER_IN_SECTIONS; x++) {
            for (int z = 0; z < IBigCube.DIAMETER_IN_SECTIONS; z++) {
                // TODO this might cause columns to reload after they've already been unloaded
                LevelChunk chunk = this.getChunk(pos.x + x, pos.z + z);
                for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
                    Heightmap heightmap = entry.getValue();
                    SurfaceTrackerWrapper tracker = (SurfaceTrackerWrapper) heightmap;
                    tracker.unloadCube(cube);
                }
            }
        }
    }

    @Override
    public void tickCube(BigCube cube, int randomTicks) {
        ProfilerFiller profilerFiller = this.getProfiler();

        // TODO lightning and snow/freezing - see ServerLevel.tickChunk()

        profilerFiller.push("tickBlocks");
        if (randomTicks > 0) {
            LevelChunkSection[] sections = cube.getCubeSections();
            CubePos cubePos = cube.getCubePos();
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection chunkSection = sections[i];
                if (chunkSection != LevelChunk.EMPTY_SECTION && chunkSection.isRandomlyTicking()) {
                    SectionPos columnPos = Coords.sectionPosByIndex(cubePos, i);
                    int minX = columnPos.minBlockX();
                    int minY = columnPos.minBlockY();
                    int minZ = columnPos.minBlockZ();
                    for (int j = 0; j < randomTicks; j++) {
                        BlockPos blockPos = this.getBlockRandomPos(minX, minY, minZ, 15);
                        profilerFiller.push("randomTick");
                        BlockState blockState = chunkSection.getBlockState(blockPos.getX() - minX, blockPos.getY() - minY, blockPos.getZ() - minZ);
                        if (blockState.isRandomlyTicking()) {
                            blockState.randomTick((ServerLevel) (Object) this, blockPos, this.random);
                        }

                        FluidState fluidState = blockState.getFluidState();
                        if (fluidState.isRandomlyTicking()) {
                            fluidState.randomTick(this, blockPos, this.random);
                        }

                        profilerFiller.pop();
                    }
                }
            }
        }
        profilerFiller.pop();
    }
}