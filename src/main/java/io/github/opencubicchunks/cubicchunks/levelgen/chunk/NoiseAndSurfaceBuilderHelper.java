package io.github.opencubicchunks.cubicchunks.levelgen.chunk;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.DummyHeightmap;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ProtoCube;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeProtoTickList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class NoiseAndSurfaceBuilderHelper extends ProtoChunk implements CubicLevelHeightAccessor {
    private final ChunkAccess[] delegates;
    private int columnX;
    private int columnZ;
    private final Map<Heightmap.Types, Heightmap> heightmaps;
    private final boolean isCubic;
    private final boolean generates2DChunks;
    private final WorldStyle worldStyle;

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    private boolean needsExtraHeight;

    public NoiseAndSurfaceBuilderHelper(CubeAccess delegate, CubeAccess delegateAbove) {
        super(delegate.getCubePos().asChunkPos(), UpgradeData.EMPTY, null, ((CubeProtoTickList<Block>) delegate.getBlockTicks()), ((CubeProtoTickList<Fluid>) delegate.getLiquidTicks()),
            new HeightAccessor(delegate));
        this.delegates = new ChunkAccess[2];
        this.delegates[0] = delegate;
        this.delegates[1] = delegateAbove;
        this.heightmaps = Maps.newEnumMap(Heightmap.Types.class);
        isCubic = ((CubicLevelHeightAccessor) delegate).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) delegate).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) delegate).worldStyle();
        this.needsExtraHeight = true;
    }

    public void setNeedsExtraHeight(boolean needsExtraHeight) {
        this.needsExtraHeight = needsExtraHeight;
    }

    public void moveColumn(int newColumnX, int newColumnZ) {
        this.columnX = newColumnX;
        this.columnZ = newColumnZ;

        for (int relativeSectionY = 0; relativeSectionY < CubeAccess.DIAMETER_IN_SECTIONS * 2; relativeSectionY++) {
            int sectionY = relativeSectionY + ((CubeAccess) delegates[0]).getCubePos().asSectionPos().getY();
            CubeAccess delegateCube = (CubeAccess) getDelegateFromSectionY(sectionY);
            assert delegateCube != null;
            getSections()[relativeSectionY] = delegateCube.getCubeSections()[Coords.sectionToIndex(newColumnX, sectionY, newColumnZ)];
        }
    }

    public void applySections() {
        for (int relativeSectionY = 0; relativeSectionY < CubeAccess.DIAMETER_IN_SECTIONS * 2; relativeSectionY++) {
            int sectionY = relativeSectionY + ((CubeAccess) delegates[0]).getCubePos().asSectionPos().getY();
            int idx = getSectionIndex(Coords.sectionToMinBlock(sectionY));
            CubeAccess delegateCube = (CubeAccess) getDelegateFromSectionY(sectionY);
            assert delegateCube != null;
            int cubeSectionIndex = Coords.sectionToIndex(columnX, sectionY, columnZ);
            LevelChunkSection cubeSection = delegateCube.getCubeSections()[cubeSectionIndex];

            if (cubeSection == null) {
                delegateCube.getCubeSections()[cubeSectionIndex] = getSections()[idx];
            }
            LevelChunkSection section = getSections()[idx];
            if (section == null) {
                getSections()[idx] = new LevelChunkSection(sectionY);
            }
        }
    }

    @Override public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, (typex) -> {
            return new DummyHeightmap(this, typex); //Essentially do nothing here.
        });
    }

    @Override public ChunkPos getPos() {
        return ((CubeAccess) delegates[0]).getCubePos().asChunkPos(columnX, columnZ);
    }

    @Override public int getHeight(Heightmap.Types type, int x, int z) {
        int blockX = Coords.localToBlock(((CubeAccess) delegates[0]).getCubePos().getX(), x) + (columnX * 16);
        int blockZ = Coords.localToBlock(((CubeAccess) delegates[0]).getCubePos().getZ(), z) + (columnZ * 16);

        CubeAccess cube1 = (CubeAccess) delegates[1];
        int localHeight = cube1.getCubeLocalHeight(type, blockX, blockZ);
        return localHeight < cube1.getCubePos().minCubeY() ? ((CubeAccess) delegates[0]).getCubeLocalHeight(type, blockX, blockZ) : localHeight;
    }

    @Override public LevelChunkSection getOrCreateSection(int sectionIndex) {
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
        return Coords.blockToCubeLocalSection(y) + CubeAccess.DIAMETER_IN_SECTIONS * getDelegateIndex(Coords.blockToCube(y));
    }

    @Override public int getMinBuildHeight() {
        return ((CubeAccess) delegates[0]).getCubePos().minCubeY();
    }

    @Override public int getSectionYFromSectionIndex(int sectionIndex) {
        int delegateIDX = sectionIndex / CubeAccess.DIAMETER_IN_SECTIONS;
        int cubeSectionIDX = sectionIndex % CubeAccess.DIAMETER_IN_SECTIONS;
        return getDelegateByIndex(delegateIDX).getCubePos().asSectionPos().getY() + cubeSectionIDX;
    }

    @Override public int getHeight() {
        return this.needsExtraHeight ? CubeAccess.DIAMETER_IN_BLOCKS + 8 : CubeAccess.DIAMETER_IN_BLOCKS;
    }

    @Override public WorldStyle worldStyle() {
        return worldStyle;
    }

    @Override public boolean isCubic() {
        return isCubic;
    }

    @Override public boolean generates2DChunks() {
        return generates2DChunks;
    }

    @Override public void addLight(BlockPos pos) {
        ChunkAccess delegate = getDelegateFromBlockY(pos.getY());
        if (delegate != null) {
            ((ProtoCube) delegate).addLight(pos);
        }
    }

    @Nullable @Override public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        ChunkAccess delegate = getDelegateFromBlockY(pos.getY());
        if (delegate != null) {
            return delegate.setBlockState(correctPos(pos), state, moved);
        }
        return Blocks.AIR.defaultBlockState();
    }

    private BlockPos correctPos(BlockPos pos) {
        return this.mutablePos.set(
            Coords.blockToSectionLocal(pos.getX()) + Coords.sectionToMinBlock(columnX),
            pos.getY(),
            Coords.blockToSectionLocal(pos.getZ()) + Coords.sectionToMinBlock(columnZ)
        );
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
        int minCubeY = ((CubeAccess) delegates[0]).getCubePos().getY();
        int maxCubeY = ((CubeAccess) delegates[1]).getCubePos().getY();

        if (cubeY < minCubeY) {
            throw StopGeneratingThrowable.INSTANCE;
        }
        if (cubeY > maxCubeY) {
            return null;
        }
        return delegates[cubeY - minCubeY];
    }

    public int getDelegateIndex(int y) {
        int minY = ((CubeAccess) delegates[0]).getCubePos().getY();
        if (y < minY) {
            return -1;
        }
        if (y > ((CubeAccess) delegates[1]).getCubePos().getY()) {
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

    @SuppressWarnings("unchecked") public <T extends ChunkAccess & CubeAccess> T getDelegateByIndex(int idx) {
        return (T) delegates[idx];
    }

    public static class StopGeneratingThrowable extends RuntimeException {
        public static final StopGeneratingThrowable INSTANCE = new StopGeneratingThrowable();

        public StopGeneratingThrowable() {
            super("Stop the surface builder");
        }
    }

    private static class HeightAccessor implements LevelHeightAccessor, CubicLevelHeightAccessor {
        private final int minBuildHeight;
        private final int height;
        private final boolean isCubic;
        private final boolean generates2DChunks;
        private final WorldStyle worldStyle;

        private HeightAccessor(ChunkAccess cube) {
            this.minBuildHeight = ((CubeAccess) cube).getCubePos().minCubeY();
            this.height = CubeAccess.DIAMETER_IN_BLOCKS * 2;
            isCubic = ((CubicLevelHeightAccessor) cube).isCubic();
            generates2DChunks = ((CubicLevelHeightAccessor) cube).generates2DChunks();
            worldStyle = ((CubicLevelHeightAccessor) cube).worldStyle();
        }

        @Override public int getHeight() {
            return height;
        }

        @Override public int getMinBuildHeight() {
            return minBuildHeight;
        }

        @Override public WorldStyle worldStyle() {
            return worldStyle;
        }

        @Override public boolean isCubic() {
            return isCubic;
        }

        @Override public boolean generates2DChunks() {
            return generates2DChunks;
        }
    }
}
