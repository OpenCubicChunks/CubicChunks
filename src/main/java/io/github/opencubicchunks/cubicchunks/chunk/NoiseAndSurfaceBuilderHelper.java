package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.DummyHeightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class NoiseAndSurfaceBuilderHelper extends ProtoChunk {


    private final ChunkAccess[] delegates;
    private int columnX;
    private int columnZ;
    private final Map<Heightmap.Types, Heightmap> heightmaps;


    public NoiseAndSurfaceBuilderHelper(IBigCube delegate, IBigCube delegateAbove) {
        super(delegate.getCubePos().asChunkPos(), UpgradeData.EMPTY, new HeightAccessor(delegate.getCubePos().minCubeY(), IBigCube.DIAMETER_IN_BLOCKS * 2));
        this.delegates = new ChunkAccess[2];
        this.delegates[0] = (ChunkAccess) delegate;
        this.delegates[1] = (ChunkAccess) delegateAbove;
        this.heightmaps = Maps.newEnumMap(Heightmap.Types.class);

    }

    public void moveColumn(int columnX, int columnZ) {
        this.columnX = columnX;
        this.columnZ = columnZ;

        for (int relativeSectionY = 0; relativeSectionY < IBigCube.DIAMETER_IN_SECTIONS * 2; relativeSectionY++) {
            int sectionY = relativeSectionY + ((IBigCube) delegates[0]).getCubePos().asSectionPos().getY();
            IBigCube delegateCube = (IBigCube) getDelegateFromSectionY(sectionY);
            assert delegateCube != null;
            getSections()[relativeSectionY] = delegateCube.getCubeSections()[Coords.sectionToIndex(columnX, sectionY, columnZ)];
        }
    }


    public void applySections() {
        for (int relativeSectionY = 0; relativeSectionY < IBigCube.DIAMETER_IN_SECTIONS * 2; relativeSectionY++) {
            int sectionY = relativeSectionY + ((IBigCube) delegates[0]).getCubePos().asSectionPos().getY();
            int idx = getSectionIndex(Coords.sectionToMinBlock(sectionY));
            IBigCube delegateCube = (IBigCube) getDelegateFromSectionY(sectionY);
            assert delegateCube != null;
            delegateCube.getCubeSections()[Coords.sectionToIndex(columnX, sectionY, columnZ)] = getSections()[idx];
            getSections()[idx] = new LevelChunkSection(sectionY);
        }
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, (typex) -> {
            return new DummyHeightmap(this, typex); //Essentially do nothing here.
        });
    }

    @Override public ChunkPos getPos() {
        return ((IBigCube) delegates[0]).getCubePos().asChunkPos(columnX, columnZ);
    }

    @Override public int getHeight(Heightmap.Types type, int x, int z) {
        int blockX = Coords.localToBlock(((IBigCube) delegates[0]).getCubePos().getX(), x) + (columnX * 16);
        int blockZ = Coords.localToBlock(((IBigCube) delegates[0]).getCubePos().getZ(), z) + (columnZ * 16);

        IBigCube cube1 = (IBigCube) delegates[1];
        int localHeight = cube1.getCubeLocalHeight(type, blockX, blockZ);
        return localHeight < cube1.getCubePos().minCubeY() ? ((IBigCube) delegates[0]).getCubeLocalHeight(type, blockX, blockZ) : localHeight;
    }

    @Override public LevelChunkSection getOrCreateSection(int sectionIndex) {
        if (sectionIndex < 0) {
            String s = "aaaaa";
            System.out.println(s);
        }


        LevelChunkSection[] cubeSections = this.getSections();

        if (cubeSections[sectionIndex] == LevelChunk.EMPTY_SECTION) {
            cubeSections[sectionIndex] = new LevelChunkSection(this.getSectionYFromSectionIndex(sectionIndex));
        }
        return cubeSections[sectionIndex];
    }


    @Override public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    @Override public int getSectionIndex(int y) {
        return Coords.blockToCubeLocalSection(y) + IBigCube.DIAMETER_IN_SECTIONS * getDelegateIndex(Coords.blockToCube(y));
    }

    @Override public int getMinBuildHeight() {
        return ((IBigCube) delegates[0]).getCubePos().minCubeY();
    }

    @Override public int getSectionYFromSectionIndex(int sectionIndex) {
        int delegateIDX = sectionIndex / IBigCube.DIAMETER_IN_SECTIONS;
        int cubeSectionIDX = sectionIndex % IBigCube.DIAMETER_IN_SECTIONS;
        return getDelegateByIndex(delegateIDX).getCubePos().asSectionPos().getY() + cubeSectionIDX;
    }


    @Override public int getHeight() {
        return IBigCube.DIAMETER_IN_BLOCKS * 2;
    }

    @Override public void addLight(BlockPos pos) {
        //TODO
    }

    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        ChunkAccess delegate = getDelegateFromBlockY(pos.getY());
        if (delegate != null) {
            return delegate.setBlockState(correctPos(pos), state, moved);
        }
        return Blocks.AIR.defaultBlockState();
    }

    private BlockPos correctPos(BlockPos pos) {
        int x = Coords.blockToSectionLocal(pos.getX()) + Coords.sectionToMinBlock(columnX);
        int z = Coords.blockToSectionLocal(pos.getZ()) + Coords.sectionToMinBlock(columnZ);
        return new BlockPos(x, pos.getY(), z);
    }

    @Override public BlockState getBlockState(BlockPos blockPos) {
        ChunkAccess delegate = getDelegateFromBlockY(blockPos.getY());
        if (delegate != null) {
            return delegate.getBlockState(correctPos(blockPos));
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override public FluidState getFluidState(BlockPos blockPos) {
        ChunkAccess delegate = getDelegateFromBlockY(blockPos.getY());
        if (delegate != null) {
            return delegate.getFluidState(correctPos(blockPos));
        }
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override public void addEntity(Entity entity) {
        ChunkAccess delegate = getDelegateFromBlockY(entity.getBlockY());
        if (delegate != null) {
            delegate.addEntity(entity);
        }
    }

    @Override public void setBlockEntity(BlockEntity blockEntity) {
        ChunkAccess delegate = getDelegateFromBlockY(blockEntity.getBlockPos().getY());
        if (delegate != null) {
            delegate.setBlockEntity(blockEntity);
        }
    }

    @Override @Nullable public BlockEntity getBlockEntity(BlockPos blockPos) {
        ChunkAccess delegate = getDelegateFromBlockY(blockPos.getY());
        if (delegate != null) {
            return delegate.getBlockEntity(correctPos(blockPos));
        }
        return null;
    }

    @Override public void removeBlockEntity(BlockPos blockPos) {
        ChunkAccess delegate = getDelegateFromBlockY(blockPos.getY());
        if (delegate != null) {
            delegate.removeBlockEntity(correctPos(blockPos));
        }
    }

    @Override public void markPosForPostprocessing(BlockPos blockPos) {
        ChunkAccess delegate = getDelegateFromBlockY(blockPos.getY());
        if (delegate != null) {
            delegate.markPosForPostprocessing(correctPos(blockPos));
        }
    }

    @Override public int getLightEmission(BlockPos blockPos) {
        ChunkAccess delegate = getDelegateFromBlockY(blockPos.getY());
        if (delegate != null) {
            return delegate.getLightEmission(correctPos(blockPos));
        }
        return 0;
    }

    /********Helpers********/
    @Nullable public ChunkAccess getDelegateCube(int cubeY) {
        int minCubeY = ((IBigCube) delegates[0]).getCubePos().getY();
        int maxCubeY = ((IBigCube) delegates[1]).getCubePos().getY();

        if (cubeY < minCubeY) {
            throw StopGeneratingThrowable.INSTANCE;
        }
        if (cubeY > maxCubeY) {
            return null;
        }
        return delegates[cubeY - minCubeY];
    }

    public int getDelegateIndex(int y) {
        int minY = ((IBigCube) delegates[0]).getCubePos().getY();
        if (y < minY) {
            return -1;
        }
        if (y > ((IBigCube) delegates[1]).getCubePos().getY()) {
            return -1;
        }
        return y - minY;
    }

    @Nullable public ChunkAccess getDelegateFromBlockY(int blockY) {
        return getDelegateCube(Coords.blockToCube(blockY));
    }

    @Nullable public ChunkAccess getDelegateFromSectionY(int sectionIDX) {
        return getDelegateCube(Coords.sectionToCube(sectionIDX));
    }

    @SuppressWarnings("unchecked") public <T extends ChunkAccess & IBigCube> T getDelegateByIndex(int idx) {
        return (T) delegates[idx];
    }

    public static class StopGeneratingThrowable extends RuntimeException {
        public static final StopGeneratingThrowable INSTANCE = new StopGeneratingThrowable();

        public StopGeneratingThrowable() {
            super("Stop the surface builder");
        }
    }

    private static class HeightAccessor implements LevelHeightAccessor {


        private final int minBuildHeight;
        private final int height;

        public HeightAccessor(int minBuildHeight, int height) {

            this.minBuildHeight = minBuildHeight;
            this.height = height;
        }

        @Override public int getHeight() {
            return height;
        }

        @Override public int getMinBuildHeight() {
            return minBuildHeight;
        }
    }
}