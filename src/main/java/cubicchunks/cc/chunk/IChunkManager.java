package cubicchunks.cc.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.cube.CubeStatus;
import cubicchunks.cc.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

public interface IChunkManager {
    int MAX_CUBE_LOADED_LEVEL = 33 + CubeStatus.maxDistance();

    int getLoadedCubesCount();

    ChunkHolder setCubeLevel(long cubePosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    LongSet getUnloadableCubes();

    ChunkHolder getCubeHolder(long cubePosIn);
    ChunkHolder getImmutableCubeHolder(long cubePosIn);

    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createCubeFuture(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn);

    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createCubeBorderFuture(ChunkHolder chunkHolder);


    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createCubeTickingFuture(ChunkHolder chunkHolder);

    CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> createCubeRegionFuture(CubePos pos, int p_219236_2_,
            IntFunction<ChunkStatus> p_219236_3_);

    CompletableFuture<Void> saveCubeScheduleTicks(Cube cubeIn);

    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createCubeEntityTickingFuture(CubePos pos);

    Iterable<ChunkHolder> getLoadedCubeIterable();

    //func_219215_b
    static int getCubeChebyshevDistance(CubePos pos, ServerPlayerEntity player, boolean p_219215_2_)  {
        int x;
        int y;
        int z;
        if (p_219215_2_) {
            //THIS IS FINE AS SECTION POS, AS IT IS CONVERTED TO CUBE POS WITH THE >> 1
            SectionPos sectionpos = player.getManagedSectionPos();
            x = sectionpos.getSectionX() >> 1;
            y = sectionpos.getSectionY() >> 1;
            z = sectionpos.getSectionZ() >> 1;
        } else {
            x = MathHelper.floor(player.getPosX() / 16.0D) >> 1;
            y = MathHelper.floor(player.getPosY() / 16.0D) >> 1;
            z = MathHelper.floor(player.getPosZ() / 16.0D) >> 1;
        }

        return getCubeDistance(pos, x, y, z);
    }

    static int getCubeDistance(CubePos cubePosIn, int x, int y, int z) {
        int dX = cubePosIn.getX() - x;
        int dY = cubePosIn.getY() - y;
        int dZ = cubePosIn.getZ() - z;
        return Math.max(Math.max(Math.abs(dX), Math.abs(dZ)), Math.abs(dY));
    }
}
