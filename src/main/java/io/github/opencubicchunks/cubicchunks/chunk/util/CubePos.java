package io.github.opencubicchunks.cubicchunks.chunk.util;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.Vec3i;

public class CubePos extends Vec3i {
    private CubePos(int xIn, int yIn, int zIn) {
        super(xIn, yIn, zIn);
    }

    public static long asLong(int x, int y, int z) {
        long i = 0L;
        i |= ((long)x & (1 << 21) - 1) << 43;
        i |= ((long)y & (1 << 22) - 1);
        i |= ((long)z & (1 << 21) - 1) << 22;
        return i;
    }

    public static CubePos of(int x, int y, int z) {
        return new CubePos(x, y, z);
    }

    public long asLong() {
        long i = 0L;
        i |= ((long)this.getX() & (1 << 21) - 1) << 43;
        i |= ((long)this.getY() & (1 << 22) - 1);
        i |= ((long)this.getZ() & (1 << 21) - 1) << 22;
        return i;
    }

    public ChunkPos asChunkPos() {
        return new ChunkPos(this.getX() << 1, this.getZ() << 1);
    }

    public static CubePos from(long cubePosIn)
    {
        return new CubePos(extractX(cubePosIn), extractY(cubePosIn), extractZ(cubePosIn));
    }
    public static CubePos from(BlockPos blockPosIn) {
        return new CubePos(blockToCube(blockPosIn.getX()), blockToCube(blockPosIn.getY()), blockToCube(blockPosIn.getZ()));
    }
    public static CubePos from(SectionPos sectionPos) { return new CubePos(sectionPos.getX() >> 1, sectionPos.getY() >> 1, sectionPos.getZ() >> 1); }

    public static CubePos from(Entity p_218157_0_) {
        return new CubePos(blockToCube(MathHelper.floor(p_218157_0_.getPosX())),
                blockToCube(MathHelper.floor(p_218157_0_.getPosY())),
                blockToCube(MathHelper.floor(p_218157_0_.getPosZ())));
    }


    public static int extractX(long packed) {
        return (int)(packed >> 43);
    }

    public static int extractY(long packed) {
        return (int)(packed << 42 >> 42);
    }

    public static int extractZ(long packed) {
        return (int)(packed << 21 >> 43);
    }

    public int minCubeX() {
        return Coords.cubeToMinBlock(getX());
    }

    public int minCubeY() {
        return Coords.cubeToMinBlock(getY());
    }

    public int minCubeZ() {
        return Coords.cubeToMinBlock(getZ());
    }

    public SectionPos asSectionPos() {
        return SectionPos.of(this.getX() << 1, this.getY() << 1, this.getZ() << 1);
    }

    public BlockPos asBlockPos() {
        return new BlockPos(minCubeX(), minCubeY(), minCubeZ());
    }

    public BlockPos asBlockPos(int localX, int localY, int localZ) {
        return new BlockPos(minCubeX() + localX, minCubeY() + localY, minCubeZ() + localZ);
    }

    public int blockX(int localX) {
        return Coords.localToBlock(getX(), localX);
    }

    public int blockY(int localY) {
        return Coords.localToBlock(getY(), localY);
    }

    public int blockZ(int localZ) {
        return Coords.localToBlock(getZ(), localZ);
    }
}
