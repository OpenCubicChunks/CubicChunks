package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld extends Level implements IServerWorld {
    protected MixinServerWorld(WritableLevelData p_i231617_1_, ResourceKey<Level> p_i231617_2_, DimensionType p_i231617_4_,
            Supplier<ProfilerFiller> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
    }

    @Override
    public void onCubeUnloading(BigCube cubeIn) {
        cubeIn.invalidateAllBlockEntities();
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