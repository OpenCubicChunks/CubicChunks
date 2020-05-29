package cubicchunks.cc.chunk.util;

import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.utils.Coords;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.Vec3i;

public class CubePos extends Vec3i {
    private CubePos(int xIn, int yIn, int zIn) {
        super(xIn, yIn, zIn);
    }

    public static long asLong(int x, int y, int z) {
        long i = 0L;
        i |= ((long)x & (1 << 21) - 1) << 43;
        i |= ((long)y & (1 << 22) - 1) << 0;
        i |= ((long)z & (1 << 21) - 1) << 22;
        return i;
    }

    public static CubePos of(int x, int y, int z) {
        return new CubePos(x, y, z);
    }

    public long asLong() {
        long i = 0L;
        i |= ((long)this.getX() & (1 << 21) - 1) << 43;
        i |= ((long)this.getY() & (1 << 22) - 1) << 0;
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
    public static CubePos from(BlockPos blockPosIn) { return new CubePos(blockPosIn.getX(), blockPosIn.getY(), blockPosIn.getZ()); }

    public static int extractX(long packed) {
        return (int)(packed << 0 >> 43);
    }

    public static int extractY(long packed) {
        return (int)(packed << 42 >> 42);
    }

    public static int extractZ(long packed) {
        return (int)(packed << 21 >> 43);
    }

    public int minCubeX() {
        return getX() * ICube.CUBEDIAMETER;
    }

    public int minCubeY() {
        return getY() * ICube.CUBEDIAMETER;
    }

    public int minCubeZ() {
        return getZ() * ICube.CUBEDIAMETER;
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
